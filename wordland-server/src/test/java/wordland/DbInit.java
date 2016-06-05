package wordland;

import org.junit.Test;

public class DbInit extends ApiClientTestBase {

    @Override public boolean seedTestData() { return false; }

    @Test public void init () throws Exception { docsEnabled = false; }

}
