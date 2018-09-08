package wordland.service.state;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import wordland.model.GameBoardBlock;
import wordland.model.GameRoom;
import wordland.model.SymbolDistribution;
import wordland.model.TurnPolicy;
import wordland.model.game.*;
import wordland.model.game.score.PlayScore;
import wordland.model.game.score.PlayScoreComponent;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static wordland.model.TurnPolicy.PARAM_MAX_TURN_DURATION;
import static wordland.model.game.GameStateChange.*;

@Slf4j
public class RedisGameStateStorageService implements GameStateStorageService {

    public static final String K_VERSION       = "version";
    public static final String K_STATE         = "state";
    public static final String K_PLAYERS       = "players";
    public static final String K_ALL_PLAYERS   = "allPlayers";
    public static final String K_PLAYER_EXITS  = "playerExits";
    public static final String K_LOG           = "log";
    public static final String K_BLOCKS        = "blocks";
    public static final String K_JOIN_ORDER    = "joinOrder";
    public static final String K_LAST_JOIN     = "lastJoin";
    public static final String K_LAST_ACTIVITY = "lastActivity";
    public static final String K_NEXT_PLAYER   = "nextPlayer";
    public static final String K_SCOREBOARD    = "scoreboard";
    public static final String K_WINNERS       = "winners";

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
        redis.set(K_STATE, RoomState.waiting.name());
        return RoomState.waiting;
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

    @Override public synchronized Map<String, GamePlayer> getCurrentAndFormerPlayers() {
        final Map<String, String> allPlayers = redis.hgetall(K_ALL_PLAYERS);
        final Map<String, GamePlayer> map = new HashMap<>();
        for (Map.Entry<String, String> entry : allPlayers.entrySet()) {
            map.put(entry.getKey(), json(entry.getValue(), GamePlayer.class));
        }
        return map;
    }

    @Override public synchronized GamePlayerExitStatus getPlayerExitStatus(String id) {
        final String json = redis.hget(K_PLAYER_EXITS, id);
        return json == null ? null : GamePlayerExitStatus.fromString(json);
    }

    @Override public synchronized Collection<GamePlayer> getPlayers() {
        final Collection<GamePlayer> players = new ArrayList<>();
        final Map<String, String> map = redis.hgetall(K_PLAYERS);
        if (map != null) {
            for (String json : map.values()) {
                players.add(json(json, GamePlayer.class));
            }
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
        final String playerId = player.getId();
        final GamePlayer found = getPlayer(playerId);
        if (found != null) return null;

        final int playerCount = getPlayerCount();
        redis.hset(K_PLAYERS, playerId, json(player));
        redis.hset(K_ALL_PLAYERS, playerId, json(player));
        if (roomSettings().hasRoundRobinPolicy()) {
            redis.set(K_JOIN_ORDER + playerCount, playerId);
        }
        redis.hset(K_SCOREBOARD, playerId, "0");
        redis.set(K_LAST_JOIN, ""+now());

        if (startGame) {
            startGame();
            return nextState(player, playerJoinedGameStarted(nextVersion(), player));
        } else {
            return nextState(player, playerJoined(nextVersion(), player));
        }
    }

    @Override public long getTimeSinceLastJoin() {
        final String val = redis.get(K_LAST_JOIN);
        return empty(val) ? 0 : now()-Long.parseLong(val);
    }

    @Override public synchronized GameStateChange removePlayer(String id) {
        final GamePlayer found = getPlayer(id);
        if (found == null) return null;
        redis.hdel(K_PLAYERS, id);
        if (redis.hget(K_PLAYER_EXITS, id) == null) {
            redis.hset(K_PLAYER_EXITS, id, GamePlayerExitStatus.abandoned.name());
        }
        if (redis.hlen(K_PLAYERS) <= 1) {
            endGame();
            return nextState(null, playerLeftGameEnded(nextVersion(), id));
        } else {
            return nextState(null, playerLeft(nextVersion(), id));
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
                                                           String word,
                                                           PlayedTile[] tiles,
                                                           PlayScore score,
                                                           Collection<String> winners) {
        final GameRoomSettings rs = validatePlayableRoomForPlayer(player);

        for (GameBoardBlock block : blocks) setBlock(block);
        incrementPlayerScore(score);

        final GameStateChange stateChange = !empty(winners)
                ? nextState(player, wordPlayedGameEnded(nextVersion(), player, word, tiles, score, winners))
                : nextState(player, wordPlayed(nextVersion(), player, word, tiles, score));

        if (stateChange.getStateChange().endsGame()) {
            redis.set(K_STATE, RoomState.ended.name());
            if (!empty(redis.get(K_WINNERS))) {
                return die("playWord: winners provided but game already ended, room: "+room.getName());
            }
            for (GamePlayer p : getPlayers()) {
                if (winners.contains(p.getId())) {
                    redis.hset(K_PLAYER_EXITS, p.getId(), GamePlayerExitStatus.won.name());
                } else if (redis.hget(K_PLAYER_EXITS, p.getId()) == null) {
                    redis.hset(K_PLAYER_EXITS, p.getId(), GamePlayerExitStatus.lost.name());
                }
            }
            redis.set(K_WINNERS, json(winners));

        } else if (rs.hasRoundRobinPolicy()) {
            // advance to next player ID
            int index = getCurrentPlayerIndex();
            final int playerCount = getPlayerCount();
            final boolean hasMinPlayersToStart = rs.hasMinPlayersToStart();
            final boolean hasMinimumPlayers = !hasMinPlayersToStart || rs.getMinPlayersToStart() <= playerCount;

            // if fewer than minimum number of players have arrived, advance to the next player that will join
            if (!hasMinimumPlayers) {
                redis.set(K_NEXT_PLAYER, ""+(index+1));
                return stateChange;
            }

            // advance to next player that has already joined (and is still playing), with wraparound
            advanceCurrentPlayer(index, playerCount);
        }

        return stateChange;
    }

    protected void advanceCurrentPlayer(int index, int playerCount) {
        for (int i=1; i<playerCount; i++) {
            index = (index + i) % playerCount;
            final String nextPlayer = getPlayerWithJoinOrder(index);
            if (nextPlayer != null && getPlayer(nextPlayer) != null) {
                redis.set(K_NEXT_PLAYER, ""+index);
            }
        }
    }

    protected GameRoomSettings validatePlayableRoomForPlayer(GamePlayer player) {
        final GameRoomSettings rs = roomSettings();
        final String roomStateJson = redis.get(K_STATE);
        if (roomStateJson == null) throw invalidEx("err.game.noState");
        switch (RoomState.valueOf(roomStateJson)) {
            case ended:
                throw invalidEx("err.game.gameOver");
            case waiting:
              if (!rs.hasRoundRobinPolicy()) {
                  throw invalidEx("err.game.waiting");
              } else {
                  // allow new players to play once after joining a round-robin game that still needs more players to officially start
                  final String currentPlayerId = getCurrentPlayerId();
                  if (currentPlayerId == null || !currentPlayerId.equals(player.getId())) {
                      throw invalidEx("err.game.notYourTurn");
                  }
              }
        }
        return rs;
    }

    private void incrementPlayerScore(PlayScore playScore) {
        if (playScore.absolute()) {
            for (PlayScoreComponent c : playScore.getScores()) {
                final String playerId = c.getPlayer();
                incrPlayerScore(playScore, playerId);
            }
        } else {
            incrPlayerScore(playScore, playScore.getPlayer().getId());
        }
    }

    private void incrPlayerScore(PlayScore playScore, String playerId) {
        final String playerScore = redis.hget(K_SCOREBOARD, playerId);
        final int score = playerScore == null ? 0 : Integer.parseInt(playerScore);
        redis.hset(K_SCOREBOARD, playerId, "" + (playScore.absolute() ? playScore.getTotal(playerId) : score + playScore.getTotal(playerId)));
    }

    @Override public GameStateChange passTurn(GamePlayer player) {
        final GameRoomSettings rs = validatePlayableRoomForPlayer(player);
        if (rs.hasRoundRobinPolicy()) {
            advanceCurrentPlayer(getCurrentPlayerIndex(), getPlayerCount());
        }
        return nextState(player, GameStateChange.turnPassed(nextVersion(), player));
    }

    @Override public synchronized Map<String, Integer> getScoreboard() {
        final Map<String, String> scoreboard = redis.hgetall(K_SCOREBOARD);
        if (empty(scoreboard)) return null;
        final Map<String, Integer> scores = new HashMap<>();
        for (Map.Entry<String, String> score : scoreboard.entrySet()) scores.put(score.getKey(), Integer.parseInt(score.getValue()));
        return scores;
    }

    @Override public synchronized Collection<String> getWinners() {
        final String[] winners = json(redis.get(K_WINNERS), String[].class);
        if (empty(winners)) {
            if (getRoomState() == RoomState.ended) {
                // highest scores are winners
                int highScore = -1;
                for (Map.Entry<String, Integer> entry : getScoreboard().entrySet()) {
                    if (entry.getValue() > highScore) highScore = entry.getValue();
                }
                final List<String> realWinners = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : getScoreboard().entrySet()) {
                    if (entry.getValue() == highScore) {
                        // they must still be in the game to win
                        final GamePlayer player = getPlayer(entry.getKey());
                        if (player != null) realWinners.add(entry.getKey());
                    }
                }
                redis.set(K_WINNERS, json(realWinners));
                return realWinners;
            }
        }
        return empty(winners) ? null : new ArrayList<>(Arrays.asList(winners));
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

    protected GameStateChange nextState(GamePlayer player, GameStateChange stateChange) {
        log.info("nextState, player="+(player == null ? "null" : player.getId()+"/"+player.getName())+"\n"+json(stateChange));
        redis.rpush(K_LOG, json(stateChange));
        if (player != null) redis.hset(K_LAST_ACTIVITY, player.getId(), ""+now());
        return stateChange;
    }

    protected void setBlock(GameBoardBlock block) {
        redis.set(K_BLOCKS + "/" + block.getBlockKey(), json(block.incrementVersion()));
    }

    @Override public synchronized List<GameStateChange> getHistory() {
        final List<String> json = redis.list(K_LOG);
        final List<GameStateChange> events = new ArrayList<>();
        for (String j : json) {
            events.add(json(j, GameStateChange.class));
        }
        return events;
    }

    @Override public synchronized List<GameStateChange> getHistory(GameStateChangeType changeType) {
        final List<String> json = redis.list(K_LOG);
        final List<GameStateChange> events = new ArrayList<>();
        for (String j : json) {
            if (changeType == null || j.contains(changeType.name())) {
                final GameStateChange change = json(j, GameStateChange.class);
                if (changeType == null || changeType.equals(change.getStateChange())) {
                    events.add(change);
                }
            }
        }
        return events;
    }

    @Override public synchronized Collection<GameStateChange> timeoutInactivePlayers() {

        final RoomState roomState = getRoomState();
        if (roomState != RoomState.active) {
            log.warn("timeoutInactivePlayers: ignoring for room "+room.getName()+" with state "+ roomState);
            return Collections.emptySet();
        }
        final Map<String, String> allPlayers = redis.hgetall(K_LAST_ACTIVITY);
        if (allPlayers.size() <= 1) return Collections.emptySet();

        final List<String> playersToBoot = new ArrayList<>();
        for (Map.Entry<String, String> entry : allPlayers.entrySet()) {
            final String playerId = entry.getKey();
            final long lastActive = Long.parseLong(entry.getValue());
            final TurnPolicy rrp = roomSettings().getRoundRobinPolicy();
            if (rrp != null) {
                if (!getCurrentPlayerId().equals(playerId)) continue;
                if (now() - lastActive > rrp.durationParam(PARAM_MAX_TURN_DURATION)) {
                    playersToBoot.add(playerId);
                }
            }
        }

        final List<GameStateChange> changes = new ArrayList<>();
        for (String playerId : playersToBoot) {
            changes.add(nextState(null, removePlayer(playerId)));
        }
        return changes;
    }
}
