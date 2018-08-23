package wordland;

import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.wizard.client.script.ApiRunner;
import org.cobbzilla.wizard.model.entityconfig.ModelSetup;
import org.cobbzilla.wizard.server.RestServer;
import wordland.server.WordlandConfiguration;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.stream2string;

public abstract class ApiModelTestBase extends ApiClientTestBase {

    public abstract String getModelPrefix();

    @Override public void onStart(RestServer<WordlandConfiguration> server) {
        super.onStart(server);
        try {
            setupModel();
        } catch (Exception e) {
            die("onStart: error calling setup: "+e, e);
        }
    }

    protected void setupModel () throws Exception {
        loginSuperuser();
        ModelSetup.setupModel(getApi(), ApiConstants.ENTITY_CONFIGS_ENDPOINT, getModelPrefix()+"/", "manifest", null, "testRun");
        logout();
    }

    protected void runScript (String scriptName) throws Exception {
        new ApiRunner(new StandardJsEngine(), getApi(), null, null)
                .run(stream2string(getModelPrefix()+"/tests/"+scriptName+".json"));
    }

}
