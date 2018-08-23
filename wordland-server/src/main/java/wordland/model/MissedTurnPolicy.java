package wordland.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MissedTurnPolicy {

    forfeit_game, forfeit_move, add_move;

    @JsonCreator public static MissedTurnPolicy fromString (String val) { return valueOf(val.toLowerCase()); }

}
