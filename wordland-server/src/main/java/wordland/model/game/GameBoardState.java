package wordland.model.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class GameBoardState {

    @Getter @Setter private long version;
    @Getter @Setter private int x1;
    @Getter @Setter private int x2;
    @Getter @Setter private int y1;
    @Getter @Setter private int y2;
    @Getter @Setter private GameTileState[][] tiles;

    public GameBoardState(long version, int x1, int x2, int y1, int y2) {
        this(version, x1, x2, y1, y2, null);
    }

    public String grid () { return GameTileState.grid(tiles); }

}
