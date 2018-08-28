package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameRoomDAO;
import wordland.model.GameRoom;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.AccountSession;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.cobbzilla.util.http.HttpContentTypes.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;
import static org.cobbzilla.wizard.resources.ResourceUtil.userPrincipal;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Service @Slf4j
public class GamesResource {

    public static final String SEP = "_";
    @Autowired private GameRoomDAO roomDAO;

    @POST @Path("/{room}")
    public Response startGame (@Context HttpContext ctx,
                               @PathParam("room") String room,
                               GameRoomSettings roomSettings) {

        final AccountSession session = userPrincipal(ctx);

        final GameRoom gameRoom = roomDAO.findByName(room);
        if (gameRoom == null) return notFound(room);

        final int sepPos = room.indexOf(SEP);
        final String roomBase = sepPos != -1 ? room.substring(0, sepPos) : room;
        final GameRoom newRoom = (GameRoom) new GameRoom(gameRoom)
                .mergeSettings(roomSettings)
                .setAccountOwner(session.getId())
                .setName(roomBase + SEP + RandomStringUtils.randomAlphanumeric(12));

        return ok(roomDAO.create(newRoom));
    }

}
