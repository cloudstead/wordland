package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum GameStateChangeType {

    player_joined, player_joined_game_started,
    player_left,   player_left_game_ended,
    turn_passed,   turn_passed_player_forfeits, turn_passed_player_forfeits_game_ended,
    word_played,   word_played_game_ended;

    @JsonCreator public static GameStateChangeType fromString(String val) { return valueOf(val.toLowerCase()); }

    public JsonNode adjustJson(JsonNode node) {
        if (node instanceof ObjectNode) {
            final JsonNode tilesJson = node.get("tilesJson");
            if (tilesJson != null) {
                ((ObjectNode) node).remove("tilesJson");
            }
        }
        return node;
    }

    public boolean playerJoined  () { return name().startsWith("player_joined"); }
    public boolean playerLeft    () { return name().startsWith("player_left"); }
    public boolean endsGame      () { return name().endsWith("_game_ended"); }
    public boolean wordPlayed    () { return name().startsWith("word_played"); }
    public boolean turnPassed    () { return name().startsWith("turn_passed"); }
    public boolean playerTurn    () { return wordPlayed() || turnPassed(); }
    public boolean playerForfeits() { return name().contains("_player_forfeits"); }

}
