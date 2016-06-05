package wordland;

import org.junit.Test;
import wordland.model.*;
import wordland.model.game.GamePlayer;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.GameRoomJoinRequest;

import static org.junit.Assert.assertNotNull;
import static wordland.ApiConstants.EP_JOIN;

public class GameRoomTest extends ApiClientTestBase {

    public static final String DOC_TARGET = "Games";

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
        final GameRoom room = findOrCreateStandardRoom();
        assertNotNull(room);
        final GameRoomJoinRequest joinRequest = new GameRoomJoinRequest();
        final GamePlayer player = post(STANDARD_ROOM_URI+EP_JOIN, joinRequest, GamePlayer.class);
        assertNotNull(player);
        assertNotNull(player.getName());
    }

}
