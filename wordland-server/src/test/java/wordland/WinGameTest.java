package wordland;

import org.junit.Test;

public class WinGameTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/5x5"; }

    @Test public void testWinMetaPressGame () throws Exception { runScript("win_metapress"); }

}
