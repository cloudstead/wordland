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

@NoArgsConstructor @Accessors(chain=true)
public class GameStateChange {

    public GameStateChange (long version, GameStateChangeType stateChange, Object object) {
        this.version = version;
        this.stateChange = stateChange;
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

    public static GameStateChange wordPlayed(long version, GamePlayer player, PlayedTile[] tiles, PlayScore score) {
        final GameRuntimeEvent event = new GameRuntimeEvent()
                .setId(player.getId())
                .setTiles(tiles)
                .setScore(score);
        return new GameStateChange(version, GameStateChangeType.word_played, event);
    }

}
