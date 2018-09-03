package wordland.model.game;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import wordland.model.GameDictionary;
import wordland.model.TileXYS;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static wordland.ApiConstants.CIRCULAR_SEARCHES;

@NoArgsConstructor @Accessors(chain=true) @EqualsAndHashCode(of={"x", "y"}, callSuper=false) @Slf4j
public class GameTileStateExtended extends GameTileState implements TileXYS {

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

    public List<GameTileStateExtended> collect(int[][] search, GameBoardState boardState) {
        final GameTileState[][] tiles = boardState.getTiles();
        final List<GameTileStateExtended> letters = new ArrayList<>();
        for (int[] offset : search) {
            final int tileX = x + offset[0];
            final int tileY = y + offset[1];
            if (tileX < 0 || tileY < 0 || tileX >= tiles.length || tileY >= tiles[0].length) continue;
            letters.add(new GameTileStateExtended(tiles[tileX][tileY], tileX, tileY));
        }
        return letters;
    }

    public <T> T findWord(GameBoardState boardState,
                          GameDictionary dictionary,
                          Set<String> playedWords,
                          Function<GameRuntimeEvent, T> function) {
        for (int[][] search : CIRCULAR_SEARCHES) {
            final List<GameTileStateExtended> letters = collect(search, boardState);
            final GameRuntimeEvent event = dictionary.findWord(letters, playedWords, this);
            if (event != null) {
                playedWords.add(event.getWord());
                for (PlayedTile t : event.getTiles()) boardState.fromRelativeTile(t);
                log.info("findWord: found '" + event.getWord() + "' with tiles: " + event.tileCoordinates());
                return function.apply(event);
            }
        }
        return null;
    }
}
