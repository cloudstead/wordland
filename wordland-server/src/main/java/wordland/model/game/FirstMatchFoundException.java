package wordland.model.game;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class FirstMatchFoundException extends TileFunctionException {
    @Getter final GameTileState tile;
    @Getter final int x;
    @Getter final int y;
}
