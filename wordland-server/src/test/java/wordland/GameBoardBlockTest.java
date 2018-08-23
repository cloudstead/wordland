package wordland;

import org.junit.Test;
import wordland.model.GameBoardBlock;

import static org.junit.Assert.assertEquals;
import static wordland.model.GameBoardBlock.BLOCK_SIZE;

public class GameBoardBlockTest {

    public static final int BS = BLOCK_SIZE;

    public int[][] TESTS = {
            // tile X  tile Y     blockX  blockY  X1     X2     Y1 Y2
            {  0,       0,         0,     0,       0,    BS-1,     0, BS-1},
            { BS,       0,         1,     0,      BS,   (BS*2)-1,  0, BS-1},
            {-BS,       0,        -1,     0,     -BS,    -1,       0, BS-1},
    };

    @Test public void testBlocks () throws Exception {
        for (int[] test : TESTS) {
            final String key = GameBoardBlock.getBlockKeyForTile(test[0], test[1]);
            final GameBoardBlock block = new GameBoardBlock(key);
            assertEquals(test[2], block.getBlockX());
            assertEquals(test[3], block.getBlockY());
            assertEquals(test[4], block.getX1());
            assertEquals(test[5], block.getX2());
            assertEquals(test[6], block.getY1());
            assertEquals(test[7], block.getY2());
        }
    }

}
