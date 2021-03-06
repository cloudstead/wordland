package wordland;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class ApiConstants {

    public static final String API_TOKEN = "x-wordland-api-key";
    public static final String EVENTS_API_TOKEN = "x-wordland-events-api-key";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGIN = "/login";
    public static final String EP_REMOVE = "/remove";

    public static final String INFO_ENDPOINT = "/info";
    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String ERR_CAPTCHA_INCORRECT = "err.captcha.incorrect";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // this is the name of many standard objects.
    public static final String STANDARD = "standard";

    // board names
    public static final String ELECTRO = "electrotype"; // 5x5 max 2 players
    public static final String WORDLANDIO = "wordlandio"; // 20x20 max 8 players
    public static final String BIG = "big";
    public static final String LARGE = "large";
    public static final String HUGE = "huge";

    public static String anonymousEmail() { return ANONYMOUS_EMAIL.replace("#STAMP#", randomAlphanumeric(10)+"-"+now()); }

    public static final String SYMBOL_SETS_ENDPOINT = "/alphabets";
    public static final String EP_POINT_SYSTEMS = "/pointSystems";
    public static final String EP_DISTRIBUTIONS = "/distributions";
    public static final String EP_DICTIONARIES = "/dictionaries";

    public static final String GAME_BOARDS_ENDPOINT = "/boards";
    public static final String GAME_ROOMS_ENDPOINT = "/rooms";
    public static final String EP_JOIN = "/join";
    public static final String EP_QUIT = "/quit";
    public static final String EP_STATE = "/state";
    public static final String EP_SETTINGS = "/settings";

}
