package wordland;

import org.junit.Test;

public class MultiRoomTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/electrotype"; }

    @Test public void checkMultiRoomStatus () throws Exception {
        runScript("multi_room");
    }
}
