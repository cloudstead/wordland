package wordland.model.game;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.json.JsonUtil;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

@NoArgsConstructor @Accessors(chain=true)
public class GameStateChange {

    public GameStateChange (int version, GameStateChangeType stateChange, Object object) {
        this.version = version;
        this.stateChange = stateChange;
        this.object = JsonUtil.FULL_MAPPER.valueToTree(object);
    }

    @Getter @Setter private int version;
    @Getter @Setter private String room;
    @Getter @Setter private GameStateChangeType stateChange;
    @Getter @Setter private JsonNode object;

    public static GameStateChange playerJoined(int version, GamePlayer player) {
        return new GameStateChange(version, GameStateChangeType.player_joined, player);
    }

    public static GameStateChange playerLeft(int version, String id) {
        return new GameStateChange(version, GameStateChangeType.player_left, id);
    }

    public static GameStateChange wordPlayed(int version, GamePlayer player, PlayedTile[] tiles) {
        return new GameStateChange(version, GameStateChangeType.word_played, new GameRuntimeEvent().setId(player.getId()).setTiles(tiles));
    }

}
