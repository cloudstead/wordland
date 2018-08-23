package wordland;

import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.wizard.client.script.ApiRunner;
import org.cobbzilla.wizard.client.script.ApiScriptIncludeClasspathHandler;
import org.cobbzilla.wizard.client.script.ApiScriptIncludeHandler;
import org.cobbzilla.wizard.model.entityconfig.ModelSetup;
import org.cobbzilla.wizard.server.RestServer;
import wordland.server.WordlandConfiguration;

import java.util.Map;

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

    private final ApiScriptIncludeHandler includeHandler = new ApiScriptIncludeClasspathHandler().setIncludePrefix(getModelPrefix()+"/tests");

    protected void runScript (String scriptName) throws Exception {
        new ApiRunner(new StandardJsEngine(), getApi(), null, includeHandler) {
            @Override public Map<String, Object> getContext() {
                final Map<String, Object> ctx = super.getContext();
                try { ctx.putAll(getServerEnvironment()); } catch (Exception e) { return die("getContext: "+e, e); }
                return ctx;
            }
        }.run(stream2string(getModelPrefix()+"/tests/"+scriptName+".json"));
    }

}
