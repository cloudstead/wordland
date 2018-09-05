package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GamePlayerExitStatus {

    won, lost, abandoned;

    @JsonCreator public static GamePlayerExitStatus fromString (String val) { return valueOf(val.toLowerCase()); }

}
