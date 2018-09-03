package wordland.model.game;

import java.util.function.Function;

public interface GameTileReducer<T> {

    T apply(GameTileState[][] tiles, int x, int y) throws Exception;

    GameTileReducer<Boolean> TRUE = (tiles, x, y) -> true;
    GameTileReducer<Integer> UNIT = (tiles, x, y) -> 1;
    GameTileReducer<Integer> FIRST = (tiles, x, y) -> { throw new FirstMatchFoundException(tiles[x][y], x, y); };

    static <T> GameTileReducer<Object> functionReducer(Function<GameTileState, T> function) {
        return (tiles, x, y) -> function.apply(tiles[x][y]);
    }

}
