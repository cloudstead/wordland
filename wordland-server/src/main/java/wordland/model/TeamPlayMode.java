package wordland.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TeamPlayMode {

    member_move, team_move;

    @JsonCreator public static TeamPlayMode fromString (String val) { return valueOf(val.toLowerCase()); }

}
