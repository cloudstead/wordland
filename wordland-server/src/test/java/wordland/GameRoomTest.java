package wordland;

import org.junit.Test;

public class GameRoomTest extends ApiModelTestBase {

    public static final String DOC_TARGET = "Games";

    @Override public String getModelPrefix() { return "models/5x5"; }

    @Test public void testCreateGameRoom () throws Exception { runScript("create_room"); }

    @Test public void testJoinRoomAndPlay () throws Exception { runScript("join_and_play"); }

    @Test public void testPlayingOutOfTurn () throws Exception { runScript("play_out_of_turn"); }

    @Test public void testPlayForWrongPlayer () throws Exception { runScript("play_for_wrong_player"); }

    @Test public void testRegisterAccount () throws Exception { runScript("register_account"); }

    @Test public void testAbandonGame () throws Exception { runScript("abandon_game"); }

}
