package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GameStateChangeType {

    player_joined, player_left, word_played, sync;

    @JsonCreator public static GameStateChangeType create (String val) { return valueOf(val.toLowerCase()); }

}
