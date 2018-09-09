package wordland.model.game;

import wordland.model.GameBoardBlock;
import wordland.model.SymbolDistribution;
import wordland.model.game.score.PlayScore;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.cobbzilla.util.json.JsonUtil.json;

public interface GameStateStorageService {

    long getVersion();

    RoomState getRoomState();
    default boolean isGameOver() {
        final RoomState state = getRoomState();
        return state == null || state == RoomState.ended;
    }

    void startGame();
    void endGame();

    GamePlayer getPlayer(String id);
    GamePlayerExitStatus getPlayerExitStatus(String uuid);
    Collection<GamePlayer> getPlayers();
    Map<String, GamePlayer> getCurrentAndFormerPlayers();
    int getPlayerCount();

    GameStateChange addPlayer(GamePlayer player);
    GameStateChange addPlayerStartGame(GamePlayer player);

    GameStateChange removePlayer(String id);

    GameBoardBlock getBlock(String blockKey);
    GameBoardBlock getBlockOrCreate(String blockKey, SymbolDistribution distribution);

    GameStateChange playWord(GamePlayer player,
                             Collection<GameBoardBlock> blocks,
                             String word,
                             PlayedTile[] tiles,
                             PlayScore score,
                             Collection<String> winners);

    GameStateChange passTurn(GamePlayer player, GameStateChangeType changeType);

    String getCurrentPlayerId();

    Map<String, Integer> getScoreboard();
    Collection<String> getWinners();

    long getTimeSinceLastJoin();

    default List<GameStateChange> getHistory() { return getHistory(p->true); }
    default List<GameStateChange> getHistory(Predicate<GameStateChange> p) { return getHistory(p, 0, 100); }
    default List<GameStateChange> getHistory(GameStateChangeType changeType) { return getHistory(p->p.getStateChange() == changeType); }
    List<GameStateChange> getHistory(Predicate<GameStateChange> p, Integer offset, Integer limit);

    default List<GameRuntimeEvent> getEvents (final GameStateChangeType changeType) {
        final List<GameStateChange> changes = getHistory(p->p.getStateChange()==changeType);
        final List<GameRuntimeEvent> events = new ArrayList<>();
        for (GameStateChange change : changes) {
            if (changeType == null || changeType.equals(change.getStateChange())) {
                events.add(json(change.getObject(), GameRuntimeEvent.class));
            }
        }
        return events;
    }

    Collection<GameStateChange> checkForMissedTurns();

    void checkCanPlay(GamePlayer player);
}
