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
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Slf4j
public class GameDaemon extends SimpleDaemon {

    @Getter private GameRoom room;
    private AtmosphereEventsService eventService;

    @Autowired private WordlandConfiguration configuration;
    @Autowired private GameDaemonContinuityService daemonContinuityService;

    private final AtomicReference<GameState> gameState = new AtomicReference<>();

    public GameDaemon(GameRoom room, AtmosphereEventsService eventService) {
        this.room = room;
        this.eventService = eventService;
    }

    public static GameDaemon templateDaemon (String room) {
        return new GameDaemon((GameRoom) new GameRoom().setTemplate(true).setName(room), null);
    }

    public GameState getGameState () {
        synchronized (gameState) {
            return gameState.get();
        }
    }

    protected GameStateStorageService getGameStateStorage() { return configuration.getGameStateStorage(room); }

    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(5);

    @Override protected long getSleepTime() { return SLEEP_TIME; }

    @Override protected void process() {
        // check for players who have timed-out, remove them from the game
        final GameStateStorageService stateStorage = getGameStateStorage();
        switch (stateStorage.getRoomState()) {
            case ended:
                return; // nothing more to do

            case waiting:
                final GameRoomSettings rs = getRoom().getSettings();
                if (stateStorage.getPlayerCount() > 0
                        && rs.hasMaxWaitBeforeBotsJoin()
                        && stateStorage.getTimeSinceLastJoin() > rs.getMillisBeforeBotsJoin()) {
                    // join bots to room
                    log.info("would join "+(rs.getMinPlayersToStart()-stateStorage.getPlayerCount())+" bots to start the game");
                }
                break;

            case active:
                // check for players that have not played in too long, boot them
        }
    }

    protected Future<Object> broadcast(GameStateChange stateChange) {
        return eventService == null ? null : eventService.broadcast(stateChange.setRoom(getRoom().getName()));
    }

    public GameDaemon initGame() {
        synchronized (gameState) {
            if (gameState.get() != null) {
                log.warn("initGame: game already started: " + room.getName());
                return this;
            }
            gameState.set(room.init(getGameStateStorage()));
            daemonContinuityService.register(this);
            start();
        }
        return this;
    }

    public boolean addPlayer(GamePlayer player) {
        final GameStateChange stateChange;
        final boolean wasAdded;
        synchronized (gameState) {
            wasAdded = gameState.get().getPlayer(player.getId()) == null;
            stateChange = gameState.get().addPlayer(player).setRoom(room.getName());
        }
        broadcast(stateChange);
        return wasAdded;
    }

    public GamePlayer findPlayer(GamePlayer player) {
        return gameState.get() == null ? null : gameState.get().getPlayer(player.getId());
    }

    public GamePlayer findPlayer(String uuid) {
        return gameState.get().getPlayer(uuid);
    }

    public void removePlayer(String id) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().removePlayer(id);
        }
        broadcast(stateChange);
    }

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().playWord(player, word, tiles);
        }
        if (stateChange != null) {
            broadcast(stateChange);
        }
        return stateChange;
    }

}
