package wordland.model.game.score;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PlayScoreComponentType {

    symbol, word, board;

    @JsonCreator public static final PlayScoreComponentType fromString (String val) { return valueOf(val.toLowerCase()); }

}
