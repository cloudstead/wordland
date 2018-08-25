package wordland.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import wordland.model.game.GameTileState;

import java.util.*;

@NoArgsConstructor @ToString(of={"blockX", "blockY"})
public class GameBoardBlock {

    public static final int BLOCK_SIZE = 32;

    public static final Comparator<GameBoardBlock> SORT_POSITION = (b1, b2) -> {
        int diff = b1.getBlockX() - b2.getBlockX();
        return diff != 0 ? diff : b1.getBlockY() - b2.getBlockY();
    };

    @Getter @Setter private int blockVersion = 0;
    public GameBoardBlock incrementVersion () { blockVersion++; return this; }

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
        int blockX = x<0 ? getNegativeKey(x) : x/BLOCK_SIZE;
        int blockY = y<0 ? getNegativeKey(y) : y/BLOCK_SIZE;
        return getBlockKey(blockX, blockY);
    }

    protected static int getNegativeKey(int index) {
        int key = (index+1) / BLOCK_SIZE;
        return key == 0 ? -1 : key - 1;
    }

    public static Collection<String> getBlockKeys(int x1, int x2, int y1, int y2) {
        final Set<String> keys = new HashSet<>();
        int x = x1;
        while (x <= x2) {
            int y = y1;
            while (y <= y2) {
                keys.add(getBlockKeyForTile(x, y));
                y += BLOCK_SIZE;
            }
            keys.add(getBlockKeyForTile(x, y2));
            x += BLOCK_SIZE;
        }
        keys.add(getBlockKeyForTile(x2, y2));
        keys.add(getBlockKeyForTile(x2, y1));
        return keys;
    }

    @JsonIgnore public int getX1() {
        if (blockX < 0) {
            return ((blockX+1) * BLOCK_SIZE) - BLOCK_SIZE;
        } else {
            return blockX * BLOCK_SIZE;
        }
    }
    @JsonIgnore public int getX2() { return getX1() + BLOCK_SIZE - 1; }
    @JsonIgnore public int getY1() {
        if (blockY < 0) {
            return ((blockY+1) * BLOCK_SIZE) - BLOCK_SIZE;
        } else {
            return blockY * BLOCK_SIZE;
        }
    }
    @JsonIgnore public int getY2() { return getY1() + BLOCK_SIZE - 1; }

    @JsonIgnore public int getWidth() { return getX2() - getX1() + 1; }
    @JsonIgnore public int getHeight() { return getY2() - getY1() + 1; }

    private void initialize(SymbolDistribution distribution) {
        final Iterator<String> picker =  distribution.getSettings().getPicker();
        final int x1 = getX1();
        final int x2 = getX2();
        final int y1 = getY1();
        final int y2 = getY2();
        tiles = new GameTileState[Math.abs(x2-x1)+1][Math.abs(y2-y1)+1];
        for (int x=0; x<tiles.length; x++) {
            for (int y=0; y<tiles[x].length; y++) {
                tiles[x][y] = new GameTileState().setSymbol(picker.next());
            }
        }
    }

    public GameTileState getAbsoluteTile(int x, int y) {
        return tiles[x - getX1()][y - getY1()];
    }

    public String grid () { return GameTileState.grid(getTiles()); }

}
