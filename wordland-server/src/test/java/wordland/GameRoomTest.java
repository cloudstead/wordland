package wordland;

import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.wizard.client.script.ApiRunner;
import org.cobbzilla.wizard.model.entityconfig.ModelSetup;
import org.junit.Test;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;

import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.junit.Assert.assertNotNull;

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

        loginSuperuser();
        ModelSetup.setupModel(getApi(), ApiConstants.ENTITY_CONFIGS_ENDPOINT, "models/electrotype/", "manifest", null, "testRun");
        logout();

        apiDocs.startRecording(DOC_TARGET, "join a game room and play a few moves, then leave");
        new ApiRunner(new StandardJsEngine(), getApi(), null, null).run(stream2string("models/electrotype/tests/join_and_play.json"));

//        apiDocs.addNote("list available rooms");
//        final GameRoom[] rooms = get(GAME_ROOMS_ENDPOINT, GameRoom[].class);
//        assertTrue(rooms.length > 0);
//
//        apiDocs.addNote("join game");
//        final GameRoomJoinRequest joinRequest = new GameRoomJoinRequest();
//        final GamePlayer player = post(STANDARD_ROOM_URI+EP_JOIN, joinRequest, GamePlayer.class);
//        assertNotNull(player);
//        assertNotNull(player.getName());
//
//        apiDocs.addNote("quit game");
//        assertEquals(HttpStatusCodes.OK, doPost(STANDARD_ROOM_URI+EP_QUIT, player.getId()).status);
    }

}
