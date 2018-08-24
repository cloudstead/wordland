package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileStateExtended extends GameTileState {

    @Getter @Setter private int x;
    @Getter @Setter private int y;

    public GameTileStateExtended(GameBoardState boardState, GameTileState tile, int x, int y) {
        super(tile);
        this.x = (boardState == null ? 0 : boardState.getX1()) + x;
        this.y = (boardState == null ? 0 : boardState.getY1()) + y;
    }

    public GameTileStateExtended(GameTileState tile, int x, int y) {
        this(null, tile, x, y);
    }

    public List<GameTileStateExtended> collect(int[][] search, GameBoardState boardState, GameTileState[][] tiles) {
        final List<GameTileStateExtended> letters = new ArrayList<>();
        for (int[] offset : search) {
            final int tileX = x + offset[0];
            final int tileY = y + offset[1];
            if (tileX < 0 || tileY < 0 || tileX >= tiles.length || tileY >= tiles[0].length) continue;
            letters.add(new GameTileStateExtended(boardState, tiles[tileX][tileY], tileX, tileY));
        }
        return letters;
    }
}
