package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoomState {

    awaiting, active, ended;

    @JsonCreator public static RoomState fromString (String val) { return valueOf(val.toLowerCase()); }

}
