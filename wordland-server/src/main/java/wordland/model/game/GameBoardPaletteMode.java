package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GameBoardPaletteMode {

    rgb, ansi;

    @JsonCreator public static GameBoardPaletteMode fromString (String val) { return valueOf(val.toLowerCase()); }

}
