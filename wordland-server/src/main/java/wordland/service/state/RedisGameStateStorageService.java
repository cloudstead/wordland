package wordland.service.state;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.wizard.cache.redis.RedisService;
import wordland.model.*;
import wordland.model.game.*;
import wordland.model.game.score.PlayScore;
import wordland.model.game.score.PlayScoreComponent;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import java.util.*;
import java.util.function.Predicate;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static wordland.model.TurnPolicy.PARAM_MAX_TURNS;
import static wordland.model.TurnPolicy.PARAM_MAX_TURN_DURATION;
import static wordland.model.TurnPolicy.PARAM_TURN_DURATION;
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
    public static final String K_LAST_WORD_PLAY= "lastWordPlay";
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
            if (!finalizeGame(winners)) return die("playWord: winners provided but game already ended, room: " + room.getName());

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

    protected boolean finalizeGame(Collection<String> winners) {
        redis.set(K_STATE, RoomState.ended.name());
        if (!empty(redis.get(K_WINNERS))) return false;

        for (GamePlayer p : getPlayers()) {
            if (winners.contains(p.getId())) {
                redis.hset(K_PLAYER_EXITS, p.getId(), GamePlayerExitStatus.won.name());
            } else if (redis.hget(K_PLAYER_EXITS, p.getId()) == null) {
                redis.hset(K_PLAYER_EXITS, p.getId(), GamePlayerExitStatus.lost.name());
            }
        }
        redis.set(K_WINNERS, json(winners));
        return true;
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
                  checkRoundRobinPlayer(player);
              }
              break;

            case active:
                if (rs.hasTurnPolicies()) {
                    if (rs.hasRoundRobinPolicy()) {
                        checkRoundRobinPlayer(player);
                    } else {
                        for (TurnPolicy p : rs.getTurnPolicies()) {
                            final Long maxTurns = p.longParam(PARAM_MAX_TURNS);
                        }
                    }
                }
        }
        return rs;
    }

    private void checkRoundRobinPlayer(GamePlayer player) {
        final String currentPlayerId = getCurrentPlayerId();
        if (currentPlayerId == null || !currentPlayerId.equals(player.getId())) {
            throw invalidEx("err.game.notYourTurn");
        }
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

    @Override public synchronized GameStateChange passTurn(GamePlayer player, GameStateChangeType changeType) {
        final GameRoomSettings rs = validatePlayableRoomForPlayer(player);
        if (changeType.playerForfeits()) {
            removePlayer(player.getId());
        }
        final int playerCount = getPlayerCount();
        if (!changeType.endsGame()) {
            if (rs.hasRoundRobinPolicy()) {
                advanceCurrentPlayer(getCurrentPlayerIndex(), playerCount);
            }
        } else {
            lastPlayerStandingWins();
        }
        return nextState(player, GameStateChange.turnPassed(nextVersion(), player, changeType));
    }

    protected void lastPlayerStandingWins() {
        // we expect only 1 player remains when this happens. verify
        final Collection<GamePlayer> players = getPlayers();
        if (players.size() != 1) die("passTurn: cannot end game when "+ players.size() +" players remain");
        finalizeGame(new SingletonList<>(players.iterator().next().getId()));
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
        if (player != null) {
            redis.hset(K_LAST_ACTIVITY, player.getId(), ""+now());
            if (stateChange.getStateChange().wordPlayed()) {
                redis.hset(K_LAST_WORD_PLAY, player.getId(), ""+now());
            }
        }
        return stateChange;
    }

    protected void setBlock(GameBoardBlock block) {
        redis.set(K_BLOCKS + "/" + block.getBlockKey(), json(block.incrementVersion()));
    }

    @Override public synchronized List<GameStateChange> getHistory() { return getHistory(p->true); }

    @Override public synchronized List<GameStateChange> getHistory(Predicate<GameStateChange> predicate) {
        return getHistory(predicate, null, null);
    }

    @Override public List<GameStateChange> getHistory(Predicate<GameStateChange> predicate, Integer offset, Integer limit) {
        final int start = offset == null || offset == 0 ? -1 : (-1*offset)-1;
        final int end = limit == null ? 0 : start - limit;
        final List<String> json = redis.lrange(K_LOG, end, start);
        if (empty(json)) return null;
        final List<GameStateChange> changes = new ArrayList<>();
        for (String j : json) {
            final GameStateChange change = json(j, GameStateChange.class);
            if (predicate.test(change)) changes.add(change);
        }
        Collections.reverse(changes);
        return changes;
    }

    @Override public synchronized Collection<GameStateChange> checkForMissedTurns() {

        final RoomState roomState = getRoomState();
        if (roomState != RoomState.active) {
            log.warn("checkForMissedTurns: ignoring for room "+room.getName()+" with state "+ roomState);
            return Collections.emptySet();
        }

        final GameRoomSettings rs = roomSettings();
        final List<String> missedTurnPlayers = new ArrayList<>();

        if (rs.hasTurnPolicies()) {
            final Map<String, String> lastWords = redis.hgetall(K_LAST_WORD_PLAY);

            // if only one person has ever played (or no one), they can't have missed their turn
            if (lastWords.size() <= 1) return Collections.emptySet();

            long minDuration = Long.MAX_VALUE;
            for (TurnPolicy p : rs.getTurnPolicies()) {
                final Long d = p.durationParam(PARAM_MAX_TURN_DURATION);
                if (d != null && d < minDuration) minDuration = d;
            }
            if (minDuration == Long.MAX_VALUE) return Collections.emptySet();

            // for round-robin, only enforce policy on current player, everyone else is waiting for them
            if (rs.hasRoundRobinPolicy()) {
                final String currentPlayerId = getCurrentPlayerId();
                checkForMissedTurn(lastWords, currentPlayerId, minDuration, missedTurnPlayers);
            } else {
                for (Map.Entry<String, String> playerPlay : lastWords.entrySet()) {
                    final String playerId = playerPlay.getKey();
                    checkForMissedTurn(lastWords, playerId, minDuration, missedTurnPlayers);
                }
            }
        }

        final List<GameStateChange> changes = new ArrayList<>();
        for (String playerId : missedTurnPlayers) {
            changes.add(nextState(null, removePlayer(playerId)));
        }
        final int numPlayers = getPlayerCount();
        if (changes.size() >= numPlayers-1) { // the game might be ending
            changes.get(changes.size()-1).setStateChange(GameStateChangeType.player_left_game_ended);
            lastPlayerStandingWins();
        }
        return changes;
    }

    private void checkForMissedTurn(Map<String, String> lastWords, String currentPlayerId, long minDuration, List<String> missedTurnPlayers) {
        final long lastPlay = Long.valueOf(lastWords.get(currentPlayerId));
        if (now() - lastPlay > minDuration) missedTurnPlayers.add(currentPlayerId);
    }

    @Override public synchronized void checkCanPlay(GamePlayer player) {
        final GameRoomSettings rs = roomSettings();
        if (rs.hasTurnPolicies()) {
            final long now = now();
            final PlayLimitCounter counter = new PlayLimitCounter();
            for (TurnPolicy turnPolicy : rs.getTurnPolicies()) {
                switch (turnPolicy.getType()) {
                    case round_robin:
                        final String nextPlayerId = getCurrentPlayerId();
                        if (nextPlayerId == null || !nextPlayerId.equals(player.getId())) throw new NotYourTurnException();
                        break;

                    case periodic_limit:
                        final int maxTurns = turnPolicy.intParam(PARAM_MAX_TURNS, 1);
                        final Long duration = turnPolicy.durationParam(PARAM_TURN_DURATION);
                        if (duration != null) counter.addCheck(turnPolicy.getName(), duration, maxTurns);
                        break;

                    default:
                        die("checkCanPlay: unrecognized TurnPolicyType: "+turnPolicy.getType());
                }
            }
            if (counter.hasChecks()) {
                counter.runChecks(player, now);
            }
        }
    }

    private class PlayLimitCounter {
        public void runChecks(GamePlayer player, long now) {
            // iterate over history, continue gathering history until we find something older than maxDuration
            final int pageSize = 100;
            int page = 0;
            final Map<Long, Integer> durationCounters = new HashMap<>();
            final Predicate<GameStateChange> wordsPlayedByPlayer = p -> p.getStateChange().wordPlayed() && p.getPlayer().getId().equals(player.getId());
            List<GameStateChange> history = getHistory(wordsPlayedByPlayer, page * pageSize, pageSize);
            boolean done = false;
            while (!done && history != null) {
                for (GameStateChange change : history) {
                    final long age = change.age(now);
                    if (age > maxDuration) {
                        done = true;
                        break;
                    }
                    for (PlayLimitCounterCheck check : checks) {
                        if (age < check.duration) {
                            final Integer counter = durationCounters.computeIfAbsent(check.duration, k -> 0);
                            durationCounters.put(check.duration, counter + 1);
                        }
                    }
                }
                if (!done) {
                    page++;
                    history = getHistory(wordsPlayedByPlayer, page * pageSize, pageSize);
                }
            }

            for (PlayLimitCounterCheck check : checks) {
                final Integer counter = durationCounters.get(check.duration);
                if (counter != null && counter >= check.maxTurns) {
                    throw new PlayRateLimitException(check.name);
                }
            }
        }

        @NoArgsConstructor @AllArgsConstructor
        private class PlayLimitCounterCheck {
            @Getter @Setter private String name;
            @Getter @Setter private long duration;
            @Getter @Setter private int maxTurns;
        }
        private List<PlayLimitCounterCheck> checks = new ArrayList<>();
        private long maxDuration;
        public boolean hasChecks() { return !checks.isEmpty(); }
        public void addCheck(String name, long duration, int maxTurns) {
            checks.add(new PlayLimitCounterCheck(name, duration, maxTurns));
            if (duration > maxDuration) maxDuration = duration;
        }
    }
}
