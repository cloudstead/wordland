package wordland.model.game;

import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static wordland.model.game.GameTileMatcher.MATCH_ALL_TILES;
import static wordland.model.game.GameTileMatcher.MATCH_CLAIMED;
import static wordland.model.game.GameTileMatcher.MATCH_UNCLAIMED;
import static wordland.model.game.GameTileReducer.*;

public class TileFunctions {

    public static <T> void forEachTile(GameTileState[][] tiles, Function<GameTileState, T> function) {
        TileMapReduce<Object, T> mapReduce = new TileMapReduce<>();
        forEachTile(tiles, mapReduce
                .setMatch(MATCH_ALL_TILES)
                .setReducer(functionReducer(function)));
    }

    public static <T, R> T forEachTile(GameTileState[][] tiles,
                                       TileMapReduce<R, T> mapReduce) {
        try {
            final GameTileMatcher match = mapReduce.getMatch();
            final GameTileReducer<R> reducer = mapReduce.getReducer();
            final boolean reduceNulls = mapReduce.isReduceNulls();
            final boolean hasAccumulator = mapReduce.hasAccumulator();
            final GameTileAccumulator<R, T> accumulator = mapReduce.getAccumulator();

            if (tiles != null) {
                for (int x = 0; x < tiles.length; x++) {
                    final GameTileState[] row = tiles[x];
                    for (int y = 0; y < row.length; y++) {
                        if (match.matches(tiles, x, y)) {
                            final R r = reducer.apply(tiles, x, y);
                            if ((r != null || reduceNulls) && hasAccumulator) accumulator.add(tiles, x, y, r);
                        }
                    }
                }
            }
            return hasAccumulator ? accumulator.getTotal() : null;

        } catch (TileFunctionException e) {
            throw e;

        } catch (Exception e) {
            return die("forEachTile: "+e.getClass().getSimpleName()+": "+e.getMessage(), e);
        }
    }

    public static int countClaimed(GameTileState[][] tiles) { return count(tiles, MATCH_CLAIMED); }

    public static int countUnclaimed(GameTileState[][] tiles) { return count(tiles, MATCH_UNCLAIMED); }

    public static int count (GameTileState[][] tiles,
                             GameTileMatcher matcher) {
        try {
            return forEachTile(tiles, new TileMapReduce<Boolean, Integer>()
                    .setMatch(matcher)
                    .setReducer(REDUCE_TRUE)
                    .setAccumulator(GameTileAccumulator.booleanCounter()));
        } catch (Exception e) {
            return die("count: "+e, e);
        }
    }

    public static GameTileStateExtended firstMatchingTile(GameTileState[][] tiles,
                                                          GameTileMatcher matcher) {
        return firstMatchingTile(tiles, matcher, 0, 0);
    }

    public static GameTileStateExtended firstMatchingTile(GameTileState[][] tiles,
                                                          GameTileMatcher matcher,
                                                          int x1, int y1) {
        try {
            return forEachTile(tiles, new TileMapReduce<Integer, GameTileStateExtended>()
                    .setX1(x1)
                    .setY1(y1)
                    .setMatch(matcher)
                    .setReducer(REDUCE_FIRST));

        } catch (FirstMatchFoundException first) {
            return new GameTileStateExtended(first.getTile(), first.getX(), first.getY());

        } catch (Exception e) {
            return die("first: "+e, e);
        }
    }

}
