package wordland;

import org.junit.Test;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;

import static org.junit.Assert.assertNotNull;
import static wordland.ApiConstants.*;

public class GameTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Games";

    @Test public void testCreateGameRoom () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create a game room with standard rules");

        final GameBoard board = get(GAME_BOARDS_ENDPOINT+"/standard", GameBoard.class);

        final String symbolSetUri = SYMBOL_SETS_ENDPOINT + "/standard";
        apiDocs.addNote("lookup standard symbol set");
        final SymbolSet symbolSet = get(symbolSetUri, SymbolSet.class);

        apiDocs.addNote("lookup standard letter distribution set");
        final SymbolDistribution defaultDistribution = get(symbolSetUri+"/"+EP_DISTRIBUTIONS+"/standard", SymbolDistribution.class);

        apiDocs.addNote("lookup standard point system");
        final PointSystem pointSystem = get(symbolSetUri+"/"+EP_POINT_SYSTEMS+"/standard", PointSystem.class);

        final GameRoomSettings roomSettings = new GameRoomSettings()
                .setBoardSettings(board.getSettings())
                .setSymbolSet(symbolSet)
                .setPointSystem(pointSystem)
                .setDefaultDistribution(defaultDistribution);

        apiDocs.addNote("login as superuser");
        loginSuperuser();

        apiDocs.addNote("create a game room");
        final GameRoom room = put(GAME_ROOMS_ENDPOINT, new GameRoom("FFA").setSettings(roomSettings));
        assertNotNull(room);
    }

}
