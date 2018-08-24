package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum GameStateChangeType {

    player_joined, player_left, word_played, sync;

    @JsonCreator public static GameStateChangeType create (String val) { return valueOf(val.toLowerCase()); }

    public JsonNode adjustJson(JsonNode node) {
        if (node instanceof ObjectNode) {
            final JsonNode tilesJson = node.get("tilesJson");
            if (tilesJson != null) {
                ((ObjectNode) node).remove("tilesJson");
            }
        }
        return node;
    }
}
