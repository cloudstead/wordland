package wordland;

import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.client.script.*;
import org.cobbzilla.wizard.model.entityconfig.ModelSetup;
import org.cobbzilla.wizard.server.RestServer;
import org.junit.Before;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.game.GameBoardState;
import wordland.model.game.GameTileState;
import wordland.model.game.GameTileStateExtended;
import wordland.model.support.GameRuntimeEvent;
import wordland.server.WordlandConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.stream2string;

public abstract class ApiModelTestBase extends ApiClientTestBase {

    public static final String FIND_WORD_AND_TILES = "find-word-and-tiles";

    public abstract String getModelPrefix();

    @Before public void resetRedis () throws Exception {
        getConfiguration().getBean(RedisService.class).flush();
    }

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
        final ApiRunnerListener listener = new ApiRunnerListenerBase("wordland-test-scripts") {
            @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {
                super.beforeCall(script, ctx);
                if (script.hasBefore()) {
                    final String before = script.getBefore();
                    if (before.startsWith(FIND_WORD_AND_TILES)) {
                        List<String> parts = StringUtil.splitAndTrim(before, " ");
                        final String tilesVar = parts.get(1);
                        final String playVar = parts.size() > 2 ? parts.get(2) : "play";

                        final GameBoardState boardState = (GameBoardState) ctx.get(tilesVar);
                        final GameTileState[][] tiles = boardState.getTiles();

                        // find a vowel..
                        GameTileStateExtended vowelTile = null;
                        for (String vowel : ApiConstants.VOWELS) {
                            for (int x=0; x<tiles.length; x++) {
                                for (int y=0; y<tiles.length; y++) {
                                    final GameTileState tile = tiles[x][y];
                                    if (tile.getSymbol().equalsIgnoreCase(vowel)) {
                                        vowelTile = new GameTileStateExtended(tile, x, y);
                                        break;
                                    }
                                }
                                if (vowelTile != null) break;
                            }
                            if (vowelTile != null) break;
                        }
                        if (vowelTile == null) die("before: "+FIND_WORD_AND_TILES+": no vowels found!");

                        // searching around the vowel, get all letters within 3 spaces
                        for (int[][] search : ApiConstants.CIRCULAR_SEARCHES) {
                            final List<GameTileStateExtended> letters = vowelTile.collect(search, boardState, tiles);
                            final GameDictionary dictionary = getServer().getConfiguration().getBean(GameDictionaryDAO.class).findDefault();
                            final GameRuntimeEvent event = dictionary.findWord(letters, playedWords);
                            if (event != null) {
                                playedWords.add(event.getWord());
                                ctx.put(playVar, event);
                                break;
                            }
                        }
                    }

                }
            }
        };
        new ApiRunnerWithEnv(getApi(), new StandardJsEngine(), listener, includeHandler, getServerEnvironment())
                .run(stream2string(getModelPrefix()+"/tests/"+scriptName+".json"));
    }

}
