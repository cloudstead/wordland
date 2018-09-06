package wordland;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.util.collection.ArrayUtil;
import wordland.model.support.AccountSession;

import static org.cobbzilla.wizard.resources.ResourceUtil.forbiddenEx;
import static org.cobbzilla.wizard.resources.ResourceUtil.userPrincipal;

public class ApiConstants {

    public static final String API_TOKEN = "x-wordland-api-key";
    public static final String EVENTS_API_TOKEN = "x-wordland-events-api-key";

    public static final String AUTH_ENDPOINT = "/auth";
    public static final String EP_LOGIN = "/login";
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGOUT = "/logout";
    public static final String REGISTER_URL = AUTH_ENDPOINT + EP_REGISTER;
    public static final String LOGIN_URL = AUTH_ENDPOINT + EP_LOGIN;

    public static final String ACCOUNTS_ENDPOINT = "/me";
    public static final String EP_REMOVE = "/remove";

    public static final String ENTITY_CONFIGS_ENDPOINT = "/entityConfigs";

    public static final String SYMBOL_SETS_ENDPOINT = "/alphabets";
    public static final String EP_POINT_SYSTEMS = "/pointSystems";
    public static final String EP_DISTRIBUTIONS = "/distributions";
    public static final String EP_DICTIONARIES = "/dictionaries";

    public static final String GAME_BOARDS_ENDPOINT = "/boards";
    public static final String GAME_ROOMS_ENDPOINT = "/rooms";
    public static final String EP_JOIN = "/join";
    public static final String EP_ABANDON = "/abandon";
    public static final String EP_PLAY = "/play";
    public static final String EP_PLAYS = "/plays";
    public static final String EP_PLAYERS = "/players";
    public static final String EP_QUIT = "/quit";
    public static final String EP_BOARD = "/board";
    public static final String EP_SCOREBOARD = "/scoreboard";
    public static final String EP_VIEW_PNG = "/view.png";
    public static final String EP_VIEW_TXT = "/view.txt";
    public static final String EP_PREVIEW_TXT = "/preview.txt";
    public static final String EP_SETTINGS = "/settings";

    public static final String INFO_ENDPOINT = "/info";
    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String ERR_CAPTCHA_INCORRECT = "err.captcha.incorrect";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // this is the name of many standard objects.
    public static final String STANDARD = "standard";

    // maximum number of rows/columns in a call to get board state
    public static final int MAX_BOARD_DETAIL_VIEW = 50;

    // max number of rows/cols to show in a PNG overview
    public static final int MAX_BOARD_VIEW = 10_000;

    public static final int[][] CIRCULAR_SEARCH_1 = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 0}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    public static final int[][] CIRCULAR_SEARCH_2 = {
            {-2, -2}, {-2, -1}, {-2, 0}, {-2, 1}, {-2, 2},
            {-1, -2}, {-1, 2},
            {0, -2}, {0, 2},
            {1, -2}, {1, 2},
            {2, -2}, {2, -1}, {2, 0}, {2, 1}, {2, 2}
    };
    public static final int[][] CIRCULAR_SEARCH_3 = {
            {-3, -3}, {-3, -2}, {-3, -1}, {-3, 0}, {-3, 1}, {-3, 2}, {-3, 3},
            {-2, -3}, {-2, 3},
            {-1, -3}, {-1, 3},
            {0, -3}, {0, 3},
            {1, -3}, {1, 3},
            {2, -3}, {2, 3},
            {3, -3}, {3, -2}, {3, -1}, {3, 0}, {3, 1}, {3, 2}, {3, 3}
    };
    public static final int[][][] CIRCULAR_SEARCHES = {
            CIRCULAR_SEARCH_1,
            ArrayUtil.concat(CIRCULAR_SEARCH_1, CIRCULAR_SEARCH_2),
            ArrayUtil.concat(CIRCULAR_SEARCH_1, CIRCULAR_SEARCH_2, CIRCULAR_SEARCH_3)
    };

    public static final String SKIP_CAPTCHA_SECRET = "skipCaptcha";

    public static AccountSession accountPrincipal(HttpContext ctx) {
        final AccountSession session = userPrincipal(ctx);
        return session.isAnonymous() ? null : session;
    }

    public static AccountSession requireAccountPrincipal(HttpContext ctx) {
        final AccountSession session = userPrincipal(ctx);
        if (session.isAnonymous()) throw forbiddenEx();
        return session;
    }

    public static AccountSession requireAdmin(HttpContext ctx) {
        final AccountSession session = userPrincipal(ctx);
        if (session.isAnonymous()) throw forbiddenEx();
        if (!session.getAccount().isAdmin()) throw forbiddenEx();
        return session;
    }
}
