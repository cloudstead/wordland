package wordland.model.game;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.json.JsonUtil;
import wordland.model.game.score.PlayScore;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

import java.util.Collection;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @Accessors(chain=true)
public class GameStateChange {

    @Getter @Setter private long version;
    @Getter @Setter private String room;
    @Getter @Setter private GameStateChangeType stateChange;
    @Getter @Setter private GamePlayer player;
    @Getter @Setter private JsonNode object;

    public GameStateChange(long version, GameStateChangeType stateChange) {
        this.version = version;
        this.stateChange = stateChange;
    }

    public GameStateChange (long version, GameStateChangeType stateChange, GamePlayer player, Object object) {
        this(version, stateChange);
        this.player = player == null ? null : player.publicView();
        this.object = stateChange.adjustJson(JsonUtil.FULL_MAPPER.valueToTree(object));
    }

    public GameStateChange (long version, GameStateChangeType stateChange, GamePlayer player) {
        this(version, stateChange, player, null);
    }

    public static GameStateChange playerJoined(long version, GamePlayer player) {
        return new GameStateChange(version, GameStateChangeType.player_joined, player, null);
    }

    public static GameStateChange playerJoinedGameStarted(long version, GamePlayer player) {
        return new GameStateChange(version, GameStateChangeType.player_joined_game_started, player, null);
    }

    public static GameStateChange playerLeft(long version, String id) {
        return new GameStateChange(version, GameStateChangeType.player_left, null, id);
    }

    public static GameStateChange playerLeftGameEnded(long version, String id) {
        return new GameStateChange(version, GameStateChangeType.player_left_game_ended, null, id);
    }

    public static GameStateChange wordPlayed(long version, GamePlayer player, String word, PlayedTile[] tiles, PlayScore score) {
        final GameRuntimeEvent event = new GameRuntimeEvent()
                .setId(player.getId())
                .setPlayer(player)
                .setWord(word)
                .setTiles(tiles)
                .setScore(score);
        return new GameStateChange(version, GameStateChangeType.word_played, player, event);
    }

    public static GameStateChange wordPlayedGameEnded(long version, GamePlayer player, String word, PlayedTile[] tiles, PlayScore score, Collection<String> winners) {
        final GameRuntimeEvent event = new GameRuntimeEvent()
                .setId(player.getId())
                .setPlayer(player)
                .setWord(word)
                .setTiles(tiles)
                .setScore(score)
                .setWinners(winners.toArray(new String[0]));
        return new GameStateChange(version, GameStateChangeType.word_played_game_ended, player, event);
    }

    public static GameStateChange turnPassed(Long version, GamePlayer player, GameStateChangeType changeType) {
        if (!changeType.turnPassed()) return die("turnPassed: invalid changeType: "+changeType);
        return new GameStateChange(version, changeType, player);
    }
}
