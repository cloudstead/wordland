package wordland;

import cloudos.model.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.cobbzilla.wizardtest.server.config.DummyRecaptchaConfig;
import wordland.auth.AccountAuthResponse;
import wordland.model.support.RegistrationRequest;
import wordland.server.DbSeedListener;
import wordland.server.WordlandConfiguration;
import wordland.server.WordlandServer;

import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static wordland.ApiConstants.*;

@Slf4j
public class ApiClientTestBase extends ApiDocsResourceIT<WordlandConfiguration, WordlandServer> {

    public static final String ENV_EXPORT_FILE = ".wordland-dev.env";

    protected String getTestConfig() { return "wordland-config-test.yml"; }

    @Override protected Map<String, String> getServerEnvironment() throws Exception {
        final Map<String, String> env = CommandShell.loadShellExports(ENV_EXPORT_FILE);
        return env;
    }

    public boolean seedTestData() { return true; }

    @Override public void onStart(RestServer<WordlandConfiguration> server) {
        // disable captcha for tests
        final WordlandConfiguration config = server.getConfiguration();
        config.setRecaptcha(DummyRecaptchaConfig.instance);
        new DbSeedListener().seed(server, seedTestData());
        super.onStart(server);
    }

    @Override protected List<ConfigurationSource> getConfigurations() {
        return new SingletonList<ConfigurationSource>(new StreamConfigurationSource(getTestConfig()));
    }

    @Override protected String getTokenHeader() { return API_TOKEN; }

    public MockTemplatedMailService getTemplatedMailService() {
        return getBean(MockTemplatedMailService.class);
    }

    public MockTemplatedMailSender getTemplatedMailSender() {
        return (MockTemplatedMailSender) getTemplatedMailService().getMailSender();
    }

    public static final String REGISTER_URL = ACCOUNTS_ENDPOINT + EP_REGISTER;
    public static final String LOGIN_URL = ACCOUNTS_ENDPOINT + EP_LOGIN;

    public AccountAuthResponse login(String email, String password) {
        AccountAuthResponse response;
        try {
            response = fromJson(post(LOGIN_URL, toJson(new LoginRequest(email, password))).json, AccountAuthResponse.class);
        } catch (Exception e) {
            return die("login: "+e, e);
        }
        pushToken(response.getSessionId());
        return response;
    }

    protected void loginSuperuser() {
        final Map<String, String> env = server.getConfiguration().getEnvironment();
        login(env.get("WORDLAND_SUPERUSER"), env.get("WORDLAND_SUPERUSER_PASS"));
    }

    public AccountAuthResponse register(RegistrationRequest request) throws Exception {
        return register(request, true);
    }
    public AccountAuthResponse register(RegistrationRequest request, boolean logout) throws Exception {
        if (logout) logout();
        AccountAuthResponse response = post(REGISTER_URL, request, AccountAuthResponse.class);
        if (response != null) pushToken(response.getSessionId());
        return response;
    }


}