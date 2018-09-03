package wordland;

import org.junit.Test;

public class MetapressTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/metapress"; }

    @Test public void testBoardScoring () throws Exception { runScript("board_scoring"); }

}
