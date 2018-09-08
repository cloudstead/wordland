package wordland;

import org.junit.Test;

public class BasicGameRoomTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/5x5"; }

    @Test public void testRegisterAccount () throws Exception { runScript("register_account"); }

    @Test public void testCreateGameRoom () throws Exception { runScript("create_room"); }

    @Test public void testJoinRoomAndPlay () throws Exception { runScript("join_and_play"); }

    @Test public void testPlayingOutOfTurn () throws Exception { runScript("play_out_of_turn"); }

    @Test public void testPlayForWrongPlayer () throws Exception { runScript("play_for_wrong_player"); }

    @Test public void testWinGame () throws Exception { runScript("win_game"); }

    @Test public void testAbandonGame () throws Exception { runScript("abandon_game"); }

    @Test public void testTimeoutPlayer () throws Exception { runScript("timeout_player"); }

    @Test public void testPassTurn () throws Exception { runScript("pass_turn"); }

}
