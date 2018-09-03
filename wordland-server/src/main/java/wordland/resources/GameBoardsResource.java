package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameBoardDAO;
import wordland.dao.GameRoomDAO;
import wordland.model.GameBoard;
import wordland.model.GameRoom;
import wordland.model.support.AccountSession;
import wordland.model.support.GenerateRoomsRequest;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;
import static wordland.ApiConstants.GAME_BOARDS_ENDPOINT;
import static wordland.ApiConstants.requireAdmin;

@Path(GAME_BOARDS_ENDPOINT)
@Service @Slf4j
public class GameBoardsResource extends NamedSystemResource<GameBoard> {

    @Getter @Autowired private GameBoardDAO dao;
    @Getter @Autowired private GameRoomDAO gameRoomDAO;

    @POST @Path("/{board}/generateRooms")
    public Response generateRooms(@Context HttpContext ctx,
                                  @PathParam("board") String boardName,
                                  @Valid GenerateRoomsRequest request) {

        final AccountSession session = requireAdmin(ctx);
        final GameBoard board = dao.findByName(boardName);
        if (board == null) return notFound(boardName);

        final List<GameRoom> rooms = new ArrayList<>();
        final Iterator<GameRoom> roomGenerator = request.getGenerator(board, gameRoomDAO.getDefaults());
        while (roomGenerator.hasNext()) {
            rooms.add(gameRoomDAO.create(roomGenerator.next()));
        }
        return ok(rooms);
    }
}