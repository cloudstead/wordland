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

    @Test public void testPlayingOutOfTurn () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "verify playing out of turn is not allowed in a round-robin game");
        runScript("play_out_of_turn");
    }

    @Test public void testPlayForWrongPlayer () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "verify playing for another player is not allowed");
        runScript("play_for_wrong_player");
    }

    @Test public void testRegisterAccount () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "register an account");
        runScript("register_account");
    }

}
