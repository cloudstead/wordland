package wordland.service;

import lombok.Getter;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.GameRoom;
import wordland.model.game.GamePlayer;
import wordland.model.game.GamePlayerState;
import wordland.model.game.GameState;
import wordland.model.game.GameStateChange;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.json;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class GameDaemon extends SimpleDaemon {

    @Getter private GameRoom room;
    private AtmosphereEventsService eventService;

    private final AtomicReference<GameState> gameState = new AtomicReference<>();
    private final AtomicReference<ArrayList<GameStateChange>> deltas = new AtomicReference<>(new ArrayList<GameStateChange>());
    private final AtomicReference<ConcurrentHashMap<String, GamePlayerState>> playerStates = new AtomicReference<>(new ConcurrentHashMap<String, GamePlayerState>());

    public GameState getGameState () {
        synchronized (gameState) {
            return new GameState(gameState.get());
        }
    }

    public GameDaemon(GameRoom room, AtmosphereEventsService eventService) {
        this.room = room;
        this.eventService = eventService;
    }

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService redis = initRedis();
    private RedisService initRedis() { return redisService.prefixNamespace("game_"+room.getName()); }

    @Autowired private GameDictionaryDAO dictDAO;

    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(5);

    @Override protected long getSleepTime() { return SLEEP_TIME; }

    @Override protected void process() {
        // periodically back up game state to redis
        synchronized (gameState) {
            getRedis().set("backup", json(gameState.get()));
        }
        // todo: check for players who have timed-out, remove them from the game
    }

    public GameDaemon startGame(GameState state) {
        synchronized (gameState) {
            gameState.set(state);
        }
        start();
        return this;
    }

    public boolean restore() {
        synchronized (gameState) {
            final String backup = getRedis().get("backup");
            if (backup == null || backup.trim().length() == 0) return false;
            gameState.set(json(backup, GameState.class));
            return true;
        }
    }

    public void addPlayer(GamePlayer player) {
        final GameStateChange stateChange;
        synchronized (gameState) {
            stateChange = gameState.get().addPlayer(player).setRoom(room.getName());
            deltas.get().add(stateChange);
            playerStates.get().put(player.getId(), new GamePlayerState());
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
            deltas.get().add(stateChange);
            playerStates.get().remove(id);
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

    public boolean isValidWord(String word) {
        GameDictionary dictionary = room.getSettings().getDictionary();
        if (dictionary == null) {
            dictionary = dictDAO.findDefault();
            if (dictionary == null) die("isValidWord: no default dictionary");
            room.getSettings().setDictionary(dictionary);
        }
        return dictionary.isWord(word);
    }

}
