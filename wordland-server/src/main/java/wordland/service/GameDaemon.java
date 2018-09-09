package wordland.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.bot.PianolaBot;
import wordland.model.GameRoom;
import wordland.model.game.*;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

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

    private final List<PianolaBot> bots = new ArrayList<>();

    @Override public void onStart() {
        // do we need to reactivate any bots?
        for (GamePlayer player : getGameStateStorage().getPlayers()) {
            if (player.bot()) startBot(player);
        }
        super.onStart();
    }

    @Override protected void process() {
        // check for players who have timed-out, remove them from the game
        final GameStateStorageService stateStorage = getGameStateStorage();
        switch (stateStorage.getRoomState()) {
            case ended:
                return; // nothing more to do

            case waiting:
                final GameRoomSettings rs = getRoom().getSettings();
                int playerCount = stateStorage.getPlayerCount();
                if (playerCount > 0
                        && playerCount < rs.getMinPlayersToStart() // ?wha? why is this room not active?
                        && rs.hasMaxWaitBeforeBotsJoin()
                        && stateStorage.getTimeSinceLastJoin() > rs.getMillisBeforeBotsJoin()) {
                    // join bots to room
                    while (playerCount < rs.getMinPlayersToStart()) {
                        log.info("would join " + (rs.getMinPlayersToStart() - playerCount) + " bots to start the game");
                        startBot();
                        playerCount = stateStorage.getPlayerCount();
                    }
                }
                break;

            case active:
                final Collection<GameStateChange> boots = stateStorage.checkForMissedTurns();
                if (!empty(boots)) {
                    for (GameStateChange boot : boots) broadcast(boot);
                }
        }
    }

    private void startBot() { startBot(null); }

    private void startBot(GamePlayer player) {
        final PianolaBot bot = new PianolaBot(this, configuration, player);
        if (player != null) {
            log.info("startBot: rejoining bot ("+player.getName()+") to room "+getRoom().getName());
        } else {
            log.info("startBot: joining new bot ("+bot.getPlayer().getName()+") to room "+getRoom().getName());
        }
        bots.add(bot.start());
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

    public GamePlayer findCurrentOrFormerPlayer(String uuid) {
        return gameState.get().getCurrentAndFormerPlayers().get(uuid);
    }

    public GamePlayerExitStatus getPlayerExitStatus(String uuid) { return gameState.get().getPlayerExitStatus(uuid); }

    public GameStateChange removePlayer(String id) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().removePlayer(id);
        }
        broadcast(stateChange);
        return stateChange;
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

    public GameStateChange passTurn(GamePlayer player) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().passTurn(player);
        }
        if (stateChange != null) {
            broadcast(stateChange);
        }
        return stateChange;
    }

}
