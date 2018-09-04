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
import wordland.dao.SessionDAO;
import wordland.model.game.GameNotificationException;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameStateChange;
import wordland.model.support.AccountSession;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandServer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.scrubStrings;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Slf4j
@ManagedService(path = AtmosphereEventsService.URI_PREFIX+"*",
                interceptors = {
                    CorsInterceptor.class,
                    IdleResourceInterceptor.class})
public class AtmosphereEventsService {

    public static final String URI_PREFIX = "/events/";
    private static final String[] SCRUB_FIELDS = {"apiToken"};

    @Getter(lazy=true) private final GamesMaster gamesMaster = initGamesMaster();
    private GamesMaster initGamesMaster() {
        return WordlandServer.WORDLAND_LIFECYCLE_LISTENER.getConfiguration().getBean(GamesMaster.class);
    }

    @Getter(lazy=true) private final SessionDAO sessionDAO = initSessionDAO();
    private SessionDAO initSessionDAO() {
        return WordlandServer.WORDLAND_LIFECYCLE_LISTENER.getConfiguration().getBean(SessionDAO.class);
    }

    @PostConstruct public void registerWithGamesMaster () { getGamesMaster().setEventService(this); }

    @Inject private BroadcasterFactory broadcasterFactory;
    public Broadcaster getBroadcaster () { return broadcasterFactory.get(); }

    private final Map<String, AtmosphereResourceEntry> clients = new ConcurrentHashMap<>(1000);
    private final Map<String, Collection<AtmosphereResourceEntry>> clientsByRoom = new ConcurrentHashMap<>(1000);

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
                final AtmosphereResourceEntry removed = clients.remove(clientId);
                if (removed != null && removed.hasRoom()) {
                    synchronized (clientsByRoom) {
                        final Collection<AtmosphereResourceEntry> clients = clientsByRoom.get(removed.getRoom());
                        if (clients != null) {
                            final Iterator<AtmosphereResourceEntry> iter = clients.iterator();
                            while (iter.hasNext()) {
                                if (iter.next().getPlayer().equals(removed.getPlayer())) {
                                    iter.remove();
                                    break;
                                }
                            }
                            if (clients.isEmpty()) clientsByRoom.remove(removed.getRoom());
                        }
                    }
                }
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
        if (player == null || !player.getId().equals(request.getId())) {
            log.warn("onMessage: no player");
            return null;
        }
        final AccountSession session = getSessionDAO().find(request.getApiToken());
        if (session == null || !session.getId().equals(request.getId())) {
            log.warn("onMessage: invalid apiKey");
            return null;
        }

        if (!request.hasStateChange()) {
            log.warn("onMessage: no state change requested, ignoring");
            return null;
        }

        switch (request.getStateChange()) {
            case player_joined:
                if (!entry.hasPlayer()) {
                    entry.setPlayer(player);
                    entry.setRoom(request.getRoom());
                    synchronized (clientsByRoom) {
                        clientsByRoom.computeIfAbsent(request.getRoom(), k -> new ArrayList<>()).add(entry);
                    }
                }
                break;

            case word_played:
                if (!request.hasTiles() || !request.hasWord()) {
                    log.warn("onMessage: no tiles sent for play");
                    return null;
                }
                final String word = request.getWord();
                final PlayedTile[] tiles = request.getTiles();
                try {
                    final GameStateChange stateChange = getGamesMaster().playWord(request.getRoom(), request.getApiToken(), player, word, tiles);
                    return stateChange != null ? json(stateChange) : null;
                } catch (GameNotificationException e) {
                    send(entry, e.getGameNotification());
                    return null; //json(notification);
                }

            default:
                log.warn("onMessage: invalid state change type: "+request.getStateChange());
                return null;
        }
        return null;
    }

    private String toMessage(Object thing) { return json(scrubStrings(thing, SCRUB_FIELDS)); }

    private void send(Collection<AtmosphereResourceEntry> entries, Object thing) {
        final String json = toMessage(thing);
        for (AtmosphereResourceEntry entry : entries) send(entry, json);
    }

    private void send(AtmosphereResourceEntry entry, Object thing) { sendMessage(entry, toMessage(thing)); }

    private void sendMessage(AtmosphereResourceEntry entry, String message) {
        try {
            log.info("send("+(entry.hasPlayer() ? entry.getPlayer().getId() : "")+", "+message+")");
            entry.getResource().getResponse().write(message);
        } catch (Exception e) {
            log.warn("send: "+e, e);
        }
    }

    public Future<Object> broadcast(GameStateChange stateChange) {
        final Collection<AtmosphereResourceEntry> entries;
        synchronized (clientsByRoom) {
            final Collection<AtmosphereResourceEntry> c = clientsByRoom.get(stateChange.getRoom());
            if (empty(c)) return null;
            entries = new ArrayList<>(c);
        }
        send(entries, stateChange);
        return null;
//         return getBroadcaster().broadcast(stateChange);
    }

    private class AtmosphereResourceEntry {
        public AtmosphereResourceEntry(AtmosphereResource resource) { this.resource = resource; }
        @Getter @Setter private AtmosphereResource resource;
        @Getter @Setter private GamePlayer player;
        @Getter @Setter private String room;
        @Getter private final long ctime = now();
        public boolean hasPlayer() { return player != null; }
        public boolean hasRoom() { return room != null; }
    }
}
