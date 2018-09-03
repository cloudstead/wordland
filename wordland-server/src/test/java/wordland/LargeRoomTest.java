package wordland;

import org.junit.Test;

public class LargeRoomTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/large"; }

    @Test public void testFillBoard () throws Exception { runScript("fill_board"); }

}
