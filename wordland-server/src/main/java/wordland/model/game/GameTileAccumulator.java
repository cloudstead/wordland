package wordland.model.game;

import lombok.Getter;

public interface GameTileAccumulator<R, T> {

    static GameTileAccumulator<Boolean, Integer> booleanCounter() {
        return new GameTileAccumulator<Boolean, Integer>() {
            @Getter private Integer total = 0;
            @Override public void add(GameTileState[][] tiles, int x, int y, Boolean reducerResult) { if (reducerResult != null && reducerResult) total++; }
        };
    }

    static GameTileAccumulator<Integer, Integer> intCounter() {
        return new GameTileAccumulator<Integer, Integer>() {
            @Getter private Integer total = 0;
            @Override public void add(GameTileState[][] tiles, int x, int y, Integer reducerResult) {
                if (reducerResult != null) total += reducerResult;
            }
        };
    }

    void add(GameTileState[][] tiles, int x, int y, R reducerResult) throws Exception;

    default T getTotal() { return null; }

}
