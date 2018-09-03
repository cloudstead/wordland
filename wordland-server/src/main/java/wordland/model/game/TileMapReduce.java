package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static wordland.model.game.TileFunctions.MATCH_ALL_TILES;

@NoArgsConstructor @Accessors(chain=true)
public class TileMapReduce<R, T> {

    @Getter @Setter private int x1;
    @Getter @Setter private int y1;
    @Getter @Setter private GameTileMatcher match = MATCH_ALL_TILES;
    @Getter @Setter private GameTileReducer<R> reducer;
    @Getter @Setter private GameTileAccumulator<R, T> accumulator;
    public boolean hasAccumulator () { return accumulator != null; }

}
