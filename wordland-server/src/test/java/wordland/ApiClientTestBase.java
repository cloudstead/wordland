package wordland;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.cobbzilla.wizardtest.server.config.DummyRecaptchaConfig;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.AccountSession;
import wordland.model.support.RegistrationRequest;
import wordland.server.WordlandConfiguration;
import wordland.server.WordlandLifecycleListener;
import wordland.server.WordlandServer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.system.CommandShell.execScript;
import static wordland.ApiConstants.*;

@Slf4j
public class ApiClientTestBase extends ApiDocsResourceIT<WordlandConfiguration, WordlandServer> {

    public static final String ENV_EXPORT_FILE = ".wl-dev.env";

    protected String getTestConfig() { return "wordland-config-test.yml"; }

    @Override protected Map<String, String> getServerEnvironment() throws Exception {
        final Map<String, String> env = CommandShell.loadShellExports(ENV_EXPORT_FILE);
        return env;
    }

    public boolean seedTestData() { return true; }

    @Override public boolean useTestSpecificDatabase() { return true; }

    @Override protected void createDb(WordlandConfiguration config, String dbName) {
        execScript(config.pgCommand("createdb", null, "postgres"), config.pgEnv());
    }

    public static final List<Integer> DROP_EXIT_VALUES = Arrays.asList(0, 1);

    @Override protected boolean dropDb (WordlandConfiguration config, String dbName, boolean background) {
        final String dropCommand = config.pgCommand("dropdb", null, "postgres");
        final String command = background ? "set -m ; " + dropCommand + " &" : dropCommand;
        execScript(command, config.pgEnv(), DROP_EXIT_VALUES);
        return true;
    }

    protected <T> T bean(Class<T> clazz) { return getServer().getConfiguration().getBean(clazz); }

    protected String handlebars(String template, Map<String, Object> ctx) {
        return HandlebarsUtil.apply(getConfiguration().getHandlebars(), template, ctx);
    }

    @Override public void beforeStart(RestServer<WordlandConfiguration> server) {
        super.beforeStart(server);
        server.getConfiguration().getRedis().setKey(null); // disable redis encryption in tests
    }

    @Override public void onStart(RestServer<WordlandConfiguration> server) {

        final WordlandConfiguration config = server.getConfiguration();

        // flush DAO caches since DB objects are reset between test runs
        config.flushDAOs();

        // disable captcha for tests
        config.setRecaptcha(DummyRecaptchaConfig.instance);

        // seed data if we should
        new WordlandLifecycleListener().seed(server, seedTestData());

        super.onStart(server);
    }

    @Override protected List<ConfigurationSource> getConfigurations() {
        return new SingletonList<>(new StreamConfigurationSource(getTestConfig()));
    }

    @Getter(lazy=true) private final ApiClientBase api = initApi();
    private ApiClientBase initApi() {
        return new ApiClientBase(super.getApi()) {
            @Override public String getTokenHeader() { return API_TOKEN; }
        };
    }

    public MockTemplatedMailService getTemplatedMailService() {
        return getBean(MockTemplatedMailService.class);
    }

    public MockTemplatedMailSender getTemplatedMailSender() {
        return (MockTemplatedMailSender) getTemplatedMailService().getMailSender();
    }

    public AccountSession login(String email, String password) {
        AccountSession response;
        try {
            response = fromJson(post(LOGIN_URL, toJson(new LoginRequest(email, password))).json, AccountSession.class);
        } catch (Exception e) {
            return die("login: "+e, e);
        }
        pushToken(response.getApiToken());
        return response;
    }

    protected void loginSuperuser() {
        final Map<String, String> env = getServer().getConfiguration().getEnvironment();
        login(env.get("WORDLAND_SUPERUSER"), env.get("WORDLAND_SUPERUSER_PASS"));
    }

    public AccountSession register(RegistrationRequest request) throws Exception {
        return register(request, true);
    }
    public AccountSession register(RegistrationRequest request, boolean logout) throws Exception {
        if (logout) logout();
        AccountSession response = post(REGISTER_URL, request, AccountSession.class);
        if (response != null) pushToken(response.getApiToken());
        return response;
    }

    public static final String STANDARD_SYMBOLS_URI = SYMBOL_SETS_ENDPOINT + "/standard";
    public static final String STANDARD_DISTRIBUTION_URI = STANDARD_SYMBOLS_URI + EP_DISTRIBUTIONS + "/standard";
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
                .setSymbolDistribution(getStandardDistribution());

        apiDocs.addNote("login as superuser");
        loginSuperuser();

        apiDocs.addNote("create standard game room, then logout superuser");
        final GameRoom standard = createRoom(STANDARD, roomSettings);

        getApi().popToken(); // logout superuser
        return standard;
    }

}