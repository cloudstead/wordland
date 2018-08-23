package wordland;

import org.junit.Test;

public class GameRoomTest extends ApiModelTestBase {

    public static final String DOC_TARGET = "Games";

    @Override public String getModelPrefix() { return "models/electrotype"; }

    @Test public void testCreateGameRoom () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create a game room with standard rules");
        runScript("create_room");
    }

    @Test public void testJoinRoomAndPlay () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "join a game room and play a few moves, then leave");
        runScript("join_and_play");
    }

}
