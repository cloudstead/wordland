package wordland;

import org.junit.Test;

public class MultiRoomTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/5x5"; }

    @Test public void checkMultiRoomStatus () throws Exception { runScript("multi_room"); }

}
