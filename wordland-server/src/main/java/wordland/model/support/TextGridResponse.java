package wordland.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.game.GameBoardPalette;

@Accessors(chain=true)
public class TextGridResponse {

    @Getter @Setter private String grid;
    @Getter @Setter private GameBoardPalette palette;
    @Getter @Setter private PlayedTile[] playedTiles;
    @Getter @Setter private Boolean success;

}
