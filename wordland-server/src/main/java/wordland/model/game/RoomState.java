package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoomState {

    awaiting_players, game_in_progress, game_ended;

    @JsonCreator public static RoomState fromString (String val) { return valueOf(val.toLowerCase()); }

}
