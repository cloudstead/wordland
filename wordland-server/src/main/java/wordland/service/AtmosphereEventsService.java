package wordland.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Message;
import org.atmosphere.config.service.Post;
import org.atmosphere.cpr.*;
import org.atmosphere.interceptor.CorsInterceptor;
import org.atmosphere.interceptor.IdleResourceInterceptor;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameStateChange;
import wordland.model.support.GameNotification;
import wordland.model.support.GameRuntimeEvent;
import wordland.server.WordlandServer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.json;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Slf4j
@ManagedService(path = AtmosphereEventsService.URI_PREFIX+"*",
                interceptors = {
                    CorsInterceptor.class,
                    IdleResourceInterceptor.class})
public class AtmosphereEventsService {

    public static final String URI_PREFIX = "/events/";

    @Getter(lazy=true) private final GamesMaster gamesMaster = initGamesMaster();
    private GamesMaster initGamesMaster() {
        return WordlandServer.WORDLAND_LIFECYCLE_LISTENER.getConfiguration().getBean(GamesMaster.class);
    }

    @PostConstruct public void registerWithGamesMaster () { getGamesMaster().setEventService(this); }

    @Inject private BroadcasterFactory broadcasterFactory;
    public Broadcaster getBroadcaster () { return broadcasterFactory.get(); }

    private final Map<String, AtmosphereResourceEntry> clients = new ConcurrentHashMap<>(1000);

    @Get
    public void onOpen(final AtmosphereResource r) {
        final String uri = r.getRequest().getRequestURI();
        final int prefix = uri.lastIndexOf(URI_PREFIX);
        if (prefix == -1 || prefix >= uri.length() - URI_PREFIX.length()) {
            log.error("invalid open request: "+uri);
            return;
        }
        final String clientId = uri.substring(prefix+URI_PREFIX.length());
        log.info("onOpen: "+ clientId);
        if (clients.containsKey(clientId)) {
            log.warn("Connection already open from "+ clientId+", replacing");
        }
        clients.put(clientId, new AtmosphereResourceEntry(r));
        r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
            @Override public void onBroadcast(AtmosphereResourceEvent event) {
                log.info("Broadcast to {} ", clientId);
                super.onBroadcast(event);
            }

            @Override public void onSuspend(AtmosphereResourceEvent event) {
                log.info("User {} connected.", clientId);
            }

            @Override public void onDisconnect(AtmosphereResourceEvent event) {
                if (event.isCancelled()) {
                    log.info("User {} unexpectedly disconnected", clientId);
                } else if (event.isClosedByClient()) {
                    log.info("User {} closed the connection", clientId);
                }
                clients.remove(clientId);
            }
        });
    }

    @Post
    public void onMessage(final AtmosphereResource r) throws IOException {
        log.info("ignoring POSTed message");
//        onMessage(r.getRequest().body().asString());
    }

    @Message
    public String onMessage(String message) throws IOException {
        log.info("onMessage: "+message);
        final GameRuntimeEvent request;
        try {
            request = json(message, GameRuntimeEvent.class);
        } catch (Exception e) {
            log.warn("unparseable message: "+message+": "+e);
            return null;
        }
        if (!request.hasClientId()) {
            log.warn("onMessage: no clientId, ignoring");
            return null;
        }
        final AtmosphereResourceEntry entry = clients.get(request.getClientId());
        if (entry == null) {
            log.warn("onMessage: client not found, ignoring");
            return null;
        }

        final GamePlayer player = getGamesMaster().findPlayer(request.getRoom(), request.getId());
        if (player == null) {
            log.warn("onMessage: no player");
            return null;
        }
        if (!player.getApiKey().equals(request.getApiKey())) {
            log.warn("onMessage: invalid apiKey");
            return null;
        }

        if (!request.hasStateChange()) {
            log.warn("onMessage: no state change requested, ignoring");
            return null;
        }

        switch (request.getStateChange()) {
            case player_joined:
                if (!entry.hasPlayer()) entry.setPlayer(player);
                break;

            case word_played:
                if (!request.hasTiles() || !request.hasWord()) {
                    log.warn("onMessage: no tiles sent for play");
                    return null;
                }
                if (!isValidWord(request.getWord())) {
                    final GameNotification notification = GameNotification.invalidWord(request.getWord());
                    send(entry, notification);
                    return null; //json(notification);
                }
                final GameStateChange stateChange = getGamesMaster().playWord(request.getRoom(), player, request.getWord(), request.getTiles());
                return stateChange != null ? json(stateChange) : null;

            default:
                log.warn("onMessage: invalid state change type: "+request.getStateChange());
                return null;
        }
        return null;
    }

    private void send(AtmosphereResourceEntry entry, Object thing) throws IOException {
//        getBroadcaster().broadcast(thing, entry.getResource());
        entry.getResource().getResponse().write(json(thing));
    }

    private boolean isValidWord(String word) {
        // todo: lookup word in dictionary in use for game
        return !word.startsWith("x");
    }

    private class AtmosphereResourceEntry {
        public AtmosphereResourceEntry(AtmosphereResource resource) { this.resource = resource; }
        @Getter @Setter private AtmosphereResource resource;
        @Getter @Setter private GamePlayer player;
        @Getter private final long ctime = now();
        public boolean hasPlayer() { return player != null; }
    }
}
