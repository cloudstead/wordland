package wordland.model.game;

import lombok.Getter;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.json.JsonUtil.json;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class GameDaemon extends SimpleDaemon {

    private String name;
    private final AtomicReference<GameState> gameState = new AtomicReference<>();

    public GameDaemon(String name) { this.name = name; }

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService redis = initRedis();
    private RedisService initRedis() { return redisService.prefixNamespace("game_"+name); }

    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(5);

    // periodically back up game state to redis
    @Override protected long getSleepTime() { return SLEEP_TIME; }

    @Override protected void process() {
        synchronized (gameState) {
            getRedis().set("backup", json(gameState.get()));
        }
    }

    public GameDaemon startGame(GameState state) {
        synchronized (gameState) {
            gameState.set(state);
        }
        start();
        return this;
    }

    private void restore() {
        synchronized (gameState) {
            gameState.set(json(getRedis().get("backup"), GameState.class));
        }
    }

    public void addPlayer(GamePlayer player) {
        synchronized (gameState) {
            gameState.get().addPlayer(player);
        }
    }

    public GamePlayer findPlayer(GamePlayer player) {
        return gameState.get().getPlayer(player.getAccount());
    }
}
