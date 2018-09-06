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

@NoArgsConstructor @Accessors(chain=true)
public class GameStateChange {

    public GameStateChange (long version, GameStateChangeType stateChange, Object object) {
        this.version = version;
        this.stateChange = stateChange;
        if (object instanceof GamePlayer) object = ((GamePlayer) object).publicView();
        this.object = stateChange.adjustJson(JsonUtil.FULL_MAPPER.valueToTree(object));
    }

    @Getter @Setter private long version;
    @Getter @Setter private String room;
    @Getter @Setter private GameStateChangeType stateChange;
    @Getter @Setter private JsonNode object;

    public static GameStateChange playerJoined(long version, GamePlayer player) {
        return new GameStateChange(version, GameStateChangeType.player_joined, player);
    }

    public static GameStateChange playerJoinedGameStarted(long version, GamePlayer player) {
        return new GameStateChange(version, GameStateChangeType.player_joined_game_started, player);
    }

    public static GameStateChange playerLeft(long version, String id) {
        return new GameStateChange(version, GameStateChangeType.player_left, id);
    }

    public static GameStateChange playerLeftGameEnded(long version, String id) {
        return new GameStateChange(version, GameStateChangeType.player_left_game_ended, id);
    }

    public static GameStateChange wordPlayed(long version, GamePlayer player, String word, PlayedTile[] tiles, PlayScore score) {
        final GameRuntimeEvent event = new GameRuntimeEvent()
                .setId(player.getId())
                .setPlayer(player)
                .setWord(word)
                .setTiles(tiles)
                .setScore(score);
        return new GameStateChange(version, GameStateChangeType.word_played, event);
    }

    public static GameStateChange wordPlayedGameEnded(long version, GamePlayer player, String word, PlayedTile[] tiles, PlayScore score, Collection<String> winners) {
        final GameRuntimeEvent event = new GameRuntimeEvent()
                .setId(player.getId())
                .setPlayer(player)
                .setWord(word)
                .setTiles(tiles)
                .setScore(score)
                .setWinners(winners.toArray(new String[0]));
        return new GameStateChange(version, GameStateChangeType.word_played_game_ended, event);
    }

}
