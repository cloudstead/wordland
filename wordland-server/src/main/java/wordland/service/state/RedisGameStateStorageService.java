package wordland.service.state;

import org.cobbzilla.wizard.cache.redis.RedisService;
import wordland.model.GameBoardBlock;
import wordland.model.GameRoom;
import wordland.model.SymbolDistribution;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameStateChange;
import wordland.model.game.GameStateStorageService;
import wordland.model.game.RoomState;
import wordland.model.game.score.PlayScore;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.model.game.GameStateChange.*;

public class RedisGameStateStorageService implements GameStateStorageService {

    public static final String K_VERSION     = "version";
    public static final String K_STATE       = "state";
    public static final String K_PLAYERS     = "players";
    public static final String K_LOG         = "log";
    public static final String K_BLOCKS      = "blocks";
    public static final String K_JOIN_ORDER  = "joinOrder";
    public static final String K_NEXT_PLAYER = "nextPlayer";
    public static final String K_SCOREBOARD  = "scoreboard";

    private GameRoom room;
    private RedisService redis;

    public GameRoomSettings roomSettings() { return room.getSettings(); }

    public RedisGameStateStorageService(RedisService redisMaster, GameRoom room) {
        final String prefix = "rooms/" + room.getName();
        this.room = room;
        this.redis = redisMaster.prefixNamespace(prefix);
    }

    @Override public synchronized RoomState getRoomState() {
        final String state = redis.get(K_STATE);
        if (state != null) return RoomState.fromString(state);
        redis.set(K_STATE, RoomState.awaiting.name());
        return RoomState.awaiting;
    }

    @Override public synchronized void startGame() {
        final RoomState roomState = getRoomState();
        if (roomState == null) die("startGame: no room state found!");
        if (roomState == RoomState.active) return; // already started
        if (roomState == RoomState.ended) die("startGame: game ended");
        redis.set(K_STATE, RoomState.active.name());
    }

    @Override public synchronized void endGame() {
        final RoomState roomState = getRoomState();
        if (roomState == null) die("endGame: no room state found!");
        if (roomState == RoomState.ended) return; // already ended
        redis.set(K_STATE, RoomState.ended.name());
    }

    @Override public synchronized GamePlayer getPlayer(String id) {
        final String json = redis.hget(K_PLAYERS, id);
        return json == null ? null : json(json, GamePlayer.class);
    }

    @Override public synchronized Collection<GamePlayer> getPlayers() {
        final Map<String, String> map = redis.hgetall(K_PLAYERS);
        final Collection<GamePlayer> players = new ArrayList<>();
        for (String json : map.values()) {
            players.add(json(json, GamePlayer.class));
        }
        return players;
    }

    @Override public synchronized int getPlayerCount() {
        final Long count = redis.hlen(K_PLAYERS);
        return count == 0 ? 0 : count.intValue();
    }

    @Override public synchronized GameStateChange addPlayer(GamePlayer player) {
        return addPlayer(player, false);
    }

    @Override public synchronized GameStateChange addPlayerStartGame(GamePlayer player) {
        return addPlayer(player, true);
    }

    private synchronized GameStateChange addPlayer(GamePlayer player, boolean startGame) {
        final GamePlayer found = getPlayer(player.getId());
        if (found != null) return null;

        final int playerCount = getPlayerCount();
        redis.hset(K_PLAYERS, player.getId(), json(player));
        if (roomSettings().hasRoundRobinPolicy()) {
            redis.set(K_JOIN_ORDER + playerCount, player.getId());
        }
        if (startGame) {
            startGame();
            return nextState(playerJoinedGameStarted(nextVersion(), player));
        } else {
            return nextState(playerJoined(nextVersion(), player));
        }
    }

    @Override public synchronized GameStateChange removePlayer(String id) {
        return removePlayer(id, false);
    }

    @Override public synchronized GameStateChange removePlayerEndGame(String id) {
        return removePlayer(id, true);
    }

    private synchronized GameStateChange removePlayer(String id, boolean endGame) {
        final GamePlayer found = getPlayer(id);
        if (found == null) return null;
        redis.hdel(K_PLAYERS, id);
        if (endGame) {
            endGame();
            return nextState(playerLeftGameEnded(nextVersion(), id));
        } else {
            return nextState(playerLeft(nextVersion(), id));
        }
    }

    @Override public synchronized GameBoardBlock getBlockOrCreate(String blockKey, SymbolDistribution distribution) {
        GameBoardBlock block = getBlock(blockKey);
        if (block == null) {
            block = new GameBoardBlock(blockKey, distribution);
            setBlock(block);
        }
        return block;
    }

    @Override public String getCurrentPlayerId() {
        if (!roomSettings().hasRoundRobinPolicy()) return null;
        return getPlayerWithJoinOrder(getCurrentPlayerIndex());
    }

    private String getPlayerWithJoinOrder(int index) { return redis.get(K_JOIN_ORDER+index); }

    protected int getCurrentPlayerIndex() {
        final String nextPlayer = redis.get(K_NEXT_PLAYER);
        return nextPlayer == null ? 0 : Integer.parseInt(nextPlayer);
    }

    @Override public synchronized GameStateChange playWord(GamePlayer player,
                                                           Collection<GameBoardBlock> blocks,
                                                           PlayedTile[] tiles,
                                                           PlayScore score) {
        for (GameBoardBlock block : blocks) setBlock(block);

        incrementPlayerScore(player, score);

        final GameStateChange stateChange = nextState(wordPlayed(nextVersion(), player, tiles, score));

        if (roomSettings().hasRoundRobinPolicy()) {
            // advance to next player ID
            int index = getCurrentPlayerIndex();
            final int playerCount = getPlayerCount();
            final boolean hasMinPlayersToStart = roomSettings().hasMinPlayersToStart();
            final boolean hasMinimumPlayers = !hasMinPlayersToStart || roomSettings().getMinPlayersToStart() <= playerCount;

            // if fewer than minimum number of players have arrived, advance to the next player that will join
            if (!hasMinimumPlayers) {
                redis.set(K_NEXT_PLAYER, ""+(index+1));
                return stateChange;
            }

            // advance to next player that has already joined (and is still playing), with wraparound
            for (int i=1; i<playerCount; i++) {
                index = (index + i) % playerCount;
                String nextPlayer = getPlayerWithJoinOrder(index);
                if (nextPlayer != null && getPlayer(nextPlayer) != null) {
                    redis.set(K_NEXT_PLAYER, ""+index);
                }
            }
        }

        return stateChange;
    }

    private void incrementPlayerScore(GamePlayer player, PlayScore playScore) {
        final String playerScore = redis.hget(K_SCOREBOARD, player.getId());
        final int score = playerScore == null ? 0 : Integer.parseInt(playerScore);
        redis.hset(K_SCOREBOARD, player.getId(), ""+(score + playScore.getTotal()));
    }

    @Override public Map<String, String> getScorebord () { return redis.hgetall(K_SCOREBOARD); }

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
        redis.set(K_BLOCKS + "/" + block.getBlockKey(), json(block.incrementVersion()));
    }
}
