package wordland;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.client.script.*;
import org.cobbzilla.wizard.model.entityconfig.ModelSetup;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import wordland.bot.Pianola;
import wordland.bot.PianolaPlay;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.game.*;
import wordland.server.WordlandConfiguration;
import wordland.service.GameDaemonContinuityService;

import java.io.*;
import java.util.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.assertTrue;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.io.FileUtil.mkdirOrDie;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.splitAndTrim;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;
import static wordland.model.game.GameTileMatcher.MATCH_VOWEL;
import static wordland.model.support.PlayedTile.claimTiles;

@Slf4j
public abstract class ApiModelTestBase extends ApiClientTestBase {

    public static final String FIND_WORD_AND_TILES = "find-word-and-tiles";
    public static final String PLAY_ALL_TILES = "play-all-tiles";
    public static final String SAVE_BOARD_VIEW = "save-board-view";
    public static final String SET_SYSTEM_CLOCK = "set-system-clock";
    public static final String RESTART_API = "restart-api";

    public static final StandardJsEngine JS = new StandardJsEngine();

    public abstract String getModelPrefix();

    @Before public void resetRedis () throws Exception { bean(RedisService.class).flush(); }

    @Override public void onStart(RestServer<WordlandConfiguration> server) {
        super.onStart(server);
        try {
            setupModel();
        } catch (Exception e) {
            die("onStart: setupModel: "+e, e);
        }
    }

    protected void setupModel () throws Exception {
        loginSuperuser();
        ModelSetup.setupModel(getApi(), ApiConstants.ENTITY_CONFIGS_ENDPOINT, getModelPrefix()+"/", "manifest", null, "testRun");
        logout();
    }

    private final ApiScriptIncludeHandler includeHandler = new ApiScriptIncludeClasspathHandler()
            .setIncludePrefix(getModelPrefix()+"/tests")
            .setCommonPath("models/");

    private Set<String> playedWords = new HashSet<>();
    @Before public void clearPlayedWords () { playedWords.clear(); }

    protected void runScript (String scriptName) throws Exception {
        final ApiClientBase api = getApi();
        final ApiRunnerListener listener = new ApiRunnerListenerBase("wordland-test-scripts") {
            @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {
                super.beforeCall(script, ctx);
                if (script.hasBefore()) {
                    final String before = script.getBefore();
                    if (before.startsWith(FIND_WORD_AND_TILES)) {
                        final List<String> parts = splitAndTrim(before, " ");
                        final String boardVar = parts.get(1);
                        final String playVar = parts.size() > 2 ? parts.get(2) : "play";
                        final int xPos = parts.size() > 3 ? JS.evaluateInt(parts.get(3), ctx) : 0;
                        final int yPos = parts.size() > 4 ? JS.evaluateInt(parts.get(4), ctx) : 0;

                        final GameBoardState boardState = (GameBoardState) ctx.get(boardVar);
                        if (boardState == null) die("beforeCall: boardVar '" + boardVar + "' is undefined");

                        // find a vowel near xPos/xPos
                        final GameTileStateExtended vowelTile = boardState.firstMatch(MATCH_VOWEL, xPos, yPos);
                        if (vowelTile == null) die("beforeCall: " + FIND_WORD_AND_TILES + ": no vowels found!");

                        final GameDictionary dictionary = bean(GameDictionaryDAO.class).findDefault();
                        vowelTile.findWord(boardState, dictionary, playedWords, (event) -> {
                            playedWords.add(event.getWord());
                            ctx.put(playVar, event);
                            log.info("selected word '" + event.getWord() + "' with tiles: " + event.tileCoordinates());
                            return event;
                        });

                    } else if (before.startsWith(PLAY_ALL_TILES)) {
                        final String stashedSession = api.getToken();
                        final List<String> parts = splitAndTrim(before, " ");
                        final String boardVar = parts.get(1);
                        final GameBoardState boardState = (GameBoardState) ctx.get(boardVar);
                        if (boardState == null) die("beforeCall: boardVar '" + boardVar + "' is undefined");

                        final String roomName = handlebars(parts.get(2), ctx);

                        final List<GamePlayer> players = new ArrayList<>();
                        for (int i=3; i<parts.size(); i++) {
                            players.add(JS.evaluate(parts.get(i), ctx));
                        }
                        if (players.isEmpty()) die("beforeCall: no players defined");

                        final GameTileState[][] tiles = boardState.getTiles();
                        // take turns playing words, using each tile on the board, until no tiles are unclaimed
                        final Pianola pianola = new Pianola(api, roomName, tiles, players);
                        final GameDictionary dictionary = bean(GameDictionaryDAO.class).findDefault();

                        pianola.setBrain((player, boardTiles, x, y) -> new GameTileStateExtended(boardTiles[x][y], x, y)
                                .findWord(boardState, dictionary, playedWords, (event) -> {
                                    claimTiles(player.getId(), tiles, event.getTiles());
                                    playedWords.add(event.getWord());
                                    log.info("selected word '" + event.getWord() + "' with tiles: " + event.tileCoordinates());
                                    return new PianolaPlay().setPlayer(player).setRoomName(roomName).setEvent(event);
                                }));

                        pianola.play(tiles);
                        int unclaimed = boardState.unclaimed();
                        assertTrue("Expected game to have been completed, unclaimed="+unclaimed, pianola.gameOver());
                        api.setToken(stashedSession);

                    } else if (before.equals(RESTART_API)) {
                        final RestServer server = getServer();
                        server.removeLifecycleListener(ApiModelTestBase.this);
                        server.stopServer();

                        // ensure tables are NOT dropped/created when server starts up
                        ((WordlandConfiguration) server.getConfiguration()).getDatabase().getHibernate().setHbm2ddlAuto("validate");

                        try {
                            server.startServer();
                        } catch (IOException e) {
                            die("beforeCall: "+RESTART_API+": error starting API: "+e, e);
                        }
                        server.addLifecycleListener(ApiModelTestBase.this);

                        final GameDaemonContinuityService continuityService = getConfiguration().getBean(GameDaemonContinuityService.class);
                        final long start = now();
                        while (!continuityService.initialized() && now() - start < SECONDS.toMillis(20)) {
                            sleep(SECONDS.toMillis(1));
                        }
                        if (!continuityService.initialized()) die("beforeCall:"+RESTART_API+": error restarting API");
                    }
                }
            }

            @Override public void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response) {
                super.afterCall(script, ctx, response);
                if (script.hasAfter()) {
                    final String after = script.getAfter();
                    if (after.startsWith(SET_SYSTEM_CLOCK)) {
                        final List<String> parts = StringUtil.splitAndTrim(after, " ");
                        final String arg = parts.get(1);
                        if (arg.startsWith("+")) {
                            final long duration = parseDuration(arg);
                            setSystemTimeOffset(duration);
                        } else {
                            final long time = (long) TimeUtil.parse(arg);
                            setSystemTimeOffset(time - ZillaRuntime.realNow());
                        }

                    } else if (after.startsWith(SAVE_BOARD_VIEW)) {
                        final String outFilePath = HandlebarsUtil.apply(getConfiguration().getHandlebars(), after.substring(SAVE_BOARD_VIEW.length()+1), ctx);
                        final File outFile = new File(outFilePath.trim());
                        final GameBoardView boardView = json(response.json, GameBoardView.class);
                        mkdirOrDie(outFile.getParentFile());
                        try (OutputStream out = new FileOutputStream(outFile)) {
                            try (InputStream in = new ByteArrayInputStream(boardView.getImage())) {
                                IOUtils.copy(in, out);
                            }
                        } catch (IOException e) {
                            die("afterCall: error writing file: " + e, e);
                        }
                    }
                }
            }
        };
        new ApiRunnerWithEnv(api, new StandardJsEngine(), listener, includeHandler, getServerEnvironment())
                .run(stream2string(getModelPrefix()+"/tests/"+scriptName+".json"));
    }

    @After public void resetClock () { setSystemTimeOffset(0); }
    @AfterClass public static void finalResetClock () { setSystemTimeOffset(0); }

}
