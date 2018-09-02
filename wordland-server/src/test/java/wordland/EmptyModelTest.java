package wordland;

import org.junit.Test;

public class EmptyModelTest extends ApiModelTestBase {

    @Override public String getModelPrefix() { return "models/empty"; }

    @Test public void testPopulateModel () throws Exception {
        runScript("populate_5x5");
    }

}
