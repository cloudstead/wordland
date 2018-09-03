package wordland;

import org.junit.Test;

public class DbInit extends ApiClientTestBase {

    @Override public boolean seedTestData() { return false; }

    @Override public boolean useTestSpecificDatabase() { return false; }

    @Test public void init () { docsEnabled = false; }

}
