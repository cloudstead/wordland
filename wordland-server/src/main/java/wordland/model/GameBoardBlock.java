package wordland.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import wordland.model.game.GameTileState;

import java.util.*;

public class GameBoardBlock {

    public static final int BLOCK_SIZE = 10;

    @Getter @Setter private int blockX;
    @Getter @Setter private int blockY;

    @Getter @Setter private GameTileState[][] tiles;

    public GameBoardBlock(int blockX, int blockY) {
        this.blockX = blockX;
        this.blockY = blockY;
    }

    public GameBoardBlock(String blockKey) {
        final String[] parts = blockKey.split("/");
        this.blockX = (Integer.parseInt(parts[0]) * 1000) + Integer.parseInt(parts[1]);
        this.blockY = (Integer.parseInt(parts[2]) * 1000) + Integer.parseInt(parts[3]);
    }

    public GameBoardBlock(String blockKey, SymbolDistribution distribution) {
        this(blockKey);
        initialize(distribution);
    }

    @JsonIgnore public String getBlockKey () { return getBlockKey(blockX, blockY); }

    public static String getBlockKey (int blockX, int blockY) {
        return (blockX / 1000) + "/" + (blockX % 1000) + "/" +
               (blockY / 1000) + "/" + (blockY % 1000);
    }

    public static String getBlockKeyForTile (int x, int y) {
        return getBlockKey(x/BLOCK_SIZE, y/BLOCK_SIZE);
    }

    public static Collection<String> getBlockKeys(int x1, int x2, int y1, int y2) {
        final Set<String> keys = new HashSet<>();
        int x = x1;
        int y = y1;
        while (x <= x2) {
            while (y <= y2) {
                keys.add(getBlockKey(x, y));
                y += BLOCK_SIZE;
            }
            keys.add(getBlockKey(x, y2));
            x += BLOCK_SIZE;
        }
        keys.add(getBlockKey(x2, y2));
        return keys;
    }

    @JsonIgnore public int getX1() { return blockX * BLOCK_SIZE; }
    @JsonIgnore public int getX2() { return getX1() + BLOCK_SIZE; }
    @JsonIgnore public int getY1() { return blockY * BLOCK_SIZE; }
    @JsonIgnore public int getY2() { return getY1() + BLOCK_SIZE; }

    private void initialize(SymbolDistribution distribution) {
        final Iterator<String> picker =  distribution.getSettings().getPicker();
        final int x1 = getX1();
        final int x2 = getX2();
        final int y1 = getY1();
        final int y2 = getY2();
        for (int x=0; x<(x2-x1); x++) {
            for (int y=0; y<(y2-y1); y++) {
                tiles[x1+x][y1+y] = new GameTileState().setSymbol(picker.next());
            }
        }
    }

    public GameTileState getAbsoluteTile(int x, int y) { return tiles[x - getX1()][y - getY1()]; }

}
