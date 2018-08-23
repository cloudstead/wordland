package wordland.service.state;

import org.cobbzilla.wizard.cache.redis.RedisService;
import wordland.model.GameBoardBlock;
import wordland.model.SymbolDistribution;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameStateChange;
import wordland.model.game.GameStateStorageService;
import wordland.model.support.PlayedTile;

import java.util.Collection;

import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.model.game.GameStateChange.*;

public class RedisGameStateStorageService implements GameStateStorageService {

    public static final String K_VERSION = "version";
    public static final String K_PLAYERS = "players";
    public static final String K_LOG     = "log";
    public static final String K_BLOCKS  = "blocks";

    private RedisService redis;

    public RedisGameStateStorageService(RedisService redisMaster, String prefix) {
        this.redis = redisMaster.prefixNamespace(prefix);
    }

    @Override public synchronized GamePlayer getPlayer(String id) {
        final String json = redis.hget(K_PLAYERS, id);
        return json == null ? null : json(json, GamePlayer.class);
    }

    @Override public synchronized int getPlayerCount() {
        final Collection<String> keys = redis.keys(K_PLAYERS);
        return keys.size();
    }

    @Override public synchronized GameStateChange addPlayer(GamePlayer player) {
        final GamePlayer found = getPlayer(player.getId());
        if (found != null) return null;
        redis.hset(K_PLAYERS, player.getId(), json(player));
        return nextState(playerJoined(nextVersion(), player));
    }

    @Override public synchronized GameStateChange removePlayer(String id) {
        final GamePlayer found = getPlayer(id);
        if (found == null) return null;
        redis.hdel(K_PLAYERS, id);
        return nextState(playerLeft(nextVersion(), id));
    }

    @Override public synchronized GameBoardBlock getBlockOrCreate(String blockKey, SymbolDistribution distribution) {
        GameBoardBlock block = getBlock(blockKey);
        if (block == null) {
            block = new GameBoardBlock(blockKey, distribution);
            setBlock(block);
        }
        return block;
    }

    @Override public synchronized GameStateChange playWord(GamePlayer player,
                                                           Collection<GameBoardBlock> blocks,
                                                           PlayedTile[] tiles) {
        for (GameBoardBlock block : blocks) setBlock(block);
        return nextState(wordPlayed(nextVersion(), player, tiles));
    }

    @Override public synchronized GameBoardBlock getBlock(String blockKey) {
        final String json = redis.get(K_BLOCKS + "/" + blockKey);
        return json == null ? null : json(json, GameBoardBlock.class);
    }

    @Override public long getVersion() {
        final Long value = redis.counterValue(K_VERSION);
        return value == null ? 0L : value;
    }

    protected Long nextVersion() { return redis.incr(K_VERSION); }

    protected GameStateChange nextState(GameStateChange stateChange) {
        redis.rpush(K_LOG, json(stateChange));
        return stateChange;
    }

    protected void setBlock(GameBoardBlock block) {
        redis.set(K_BLOCKS + "/" + block.getBlockKey(), json(block));
    }
}
