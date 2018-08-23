package wordland.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.model.GameRoom;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameState;
import wordland.model.game.GameStateChange;
import wordland.model.game.GameStateStorageService;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Slf4j
public class GameDaemon extends SimpleDaemon {

    @Getter private GameRoom room;
    private AtmosphereEventsService eventService;

    private final AtomicReference<GameState> gameState = new AtomicReference<>();

    public GameState getGameState () {
        synchronized (gameState) {
            return gameState.get();
        }
    }

    protected GameStateStorageService getGameStateStorage() { return configuration.getGameStateStorage(room.getName()); }

    public GameDaemon(GameRoom room, AtmosphereEventsService eventService) {
        this.room = room;
        this.eventService = eventService;
    }

    @Autowired private WordlandConfiguration configuration;
    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(5);

    @Override protected long getSleepTime() { return SLEEP_TIME; }

    @Override protected void process() {
        // todo: check for players who have timed-out, remove them from the game
    }

    public GameDaemon startGame() {
        if (gameState.get() != null) {
            log.warn("startGame: game already started: "+room.getName());
            return this;
        }
        gameState.set(room.initializeBoard(getGameStateStorage()));
        start();
        return this;
    }

    public void addPlayer(GamePlayer player) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().addPlayer(player).setRoom(room.getName());
        }
        eventService.getBroadcaster().broadcast(stateChange);
    }

    public GamePlayer findPlayer(GamePlayer player) {
        return gameState.get().getPlayer(player.getAccount());
    }

    public GamePlayer findPlayer(String uuid) {
        return gameState.get().getPlayer(uuid);
    }

    public void removePlayer(String id) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().removePlayer(id);
        }
        eventService.getBroadcaster().broadcast(stateChange);
    }

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().playWord(player, word, tiles);
        }
        if (stateChange != null) {
            eventService.getBroadcaster().broadcast(stateChange);
        }
        return stateChange;
    }

}
