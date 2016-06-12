package wordland;

import cloudos.model.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.cobbzilla.wizardtest.server.config.DummyRecaptchaConfig;
import wordland.auth.AccountAuthResponse;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.RegistrationRequest;
import wordland.server.WordlandLifecycleListener;
import wordland.server.WordlandConfiguration;
import wordland.server.WordlandServer;

import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.json;
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
        new WordlandLifecycleListener().seed(server, seedTestData());
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

    public static final String STANDARD_SYMBOLS_URI = SYMBOL_SETS_ENDPOINT + "/standard";
    public static final String STANDARD_DISTRIBUTION_URI = STANDARD_SYMBOLS_URI + "/" + EP_DISTRIBUTIONS + "/standard";
    public static final String STANDARD_ROOM_URI = GAME_ROOMS_ENDPOINT + "/standard";

    public GameRoom createRoom(String name, GameRoomSettings roomSettings) throws Exception {
        return put(GAME_ROOMS_ENDPOINT, new GameRoom(name).setSettings(roomSettings));
    }

    public GameBoard getStandardGameBoard() throws Exception {
        return get(GAME_BOARDS_ENDPOINT+"/standard", GameBoard.class);
    }

    public PointSystem getStandardPointSystem() throws Exception {
        return get(STANDARD_SYMBOLS_URI +"/"+EP_POINT_SYSTEMS+"/standard", PointSystem.class);
    }

    public SymbolDistribution getStandardDistribution() throws Exception {
        return get(STANDARD_DISTRIBUTION_URI, SymbolDistribution.class);
    }

    public SymbolSet getStandardSymbolSet() throws Exception {
        return get(STANDARD_SYMBOLS_URI, SymbolSet.class);
    }

    public GameRoom findOrCreateStandardRoom() throws Exception {
        final RestResponse response = doGet(STANDARD_ROOM_URI);
        if (response.status == 200) return json(response.json, GameRoom.class);
        if (response.status != 404) throw new ApiException(response);

        final GameRoomSettings roomSettings = new GameRoomSettings()
                .setBoard(getStandardGameBoard())
                .setSymbolSet(getStandardSymbolSet())
                .setPointSystem(getStandardPointSystem())
                .setDefaultDistribution(getStandardDistribution());

        apiDocs.addNote("login as superuser");
        loginSuperuser();

        apiDocs.addNote("create standard game room, then logout superuser");
        final GameRoom standard = createRoom(STANDARD, roomSettings);

        popToken(); // logout superuser
        return standard;
    }

}