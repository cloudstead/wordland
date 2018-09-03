package wordland;

import org.junit.Test;

public class InfinityRoomTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/infinity"; }

    @Test public void testPlayGameOnInfinityBoard () throws Exception { runScript("play_infinity"); }

    @Test public void testBoardView () throws Exception { runScript("board_view"); }

}
