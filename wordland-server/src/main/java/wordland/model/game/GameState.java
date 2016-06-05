package wordland.model.game;

import lombok.Getter;

public class GameState {

    @Getter private GameTileState[][] tiles;

    public GameState(int length, int width) {
        tiles = new GameTileState[length][width];
        for (int x=0; x<length; x++) {
            for (int y=0; y<width; y++) {
                tiles[x][y] = new GameTileState();
            }
        }
    }

    public void setTileSymbol(int x, int y, String symbol) {
        tiles[x][y].setSymbol(symbol);
    }
}
