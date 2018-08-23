package wordland;

import org.junit.Test;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;

import static org.junit.Assert.assertNotNull;

public class GameRoomTest extends ApiModelTestBase {

    public static final String DOC_TARGET = "Games";

    @Override public String getModelPrefix() { return "models/electrotype"; }

    @Test public void testCreateGameRoom () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create a game room with standard rules");

        apiDocs.addNote("lookup standard game board");
        final GameBoard board = getStandardGameBoard();

        apiDocs.addNote("lookup standard symbol set");
        final SymbolSet symbolSet = getStandardSymbolSet();

        apiDocs.addNote("lookup standard letter distribution set");
        final SymbolDistribution defaultDistribution = getStandardDistribution();

        apiDocs.addNote("lookup standard point system");
        final PointSystem pointSystem = getStandardPointSystem();

        final GameRoomSettings roomSettings = new GameRoomSettings()
                .setBoard(board)
                .setSymbolSet(symbolSet)
                .setPointSystem(pointSystem)
                .setDefaultDistribution(defaultDistribution);

        apiDocs.addNote("login as superuser");
        loginSuperuser();

        apiDocs.addNote("create a game room");
        final GameRoom room = createRoom("FFA", roomSettings);
        assertNotNull(room);
    }

    @Test public void testJoinRoomAndPlay () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "join a game room and play a few moves, then leave");
        runScript("join_and_play");
    }

}
