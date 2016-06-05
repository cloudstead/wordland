package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameRoomDAO;
import wordland.model.Account;
import wordland.model.GameRoom;
import wordland.model.game.GamePlayer;
import wordland.model.support.GameRoomJoinRequest;
import wordland.service.GamesMaster;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;
import static org.cobbzilla.wizard.resources.ResourceUtil.optionalUserPrincipal;
import static wordland.ApiConstants.*;

@Path(GAME_ROOMS_ENDPOINT)
@Service @Slf4j
public class GameRoomsResource extends NamedSystemResource<GameRoom> {

    @Getter @Autowired private GameRoomDAO dao;
    @Getter @Autowired private GamesMaster gamesMaster;

    @POST
    @Path("/{name}"+EP_JOIN)
    public Response join (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          @Valid GameRoomJoinRequest request) {

        final Account account = optionalUserPrincipal(ctx);
        final GamePlayer player = new GamePlayer(account, request);

        final GamePlayer found = gamesMaster.findPlayer(room, player);
        if (found != null) return ok(found);

        gamesMaster.addPlayer(room, player);
        return ok(player);
    }

    @POST
    @Path("/{name}"+EP_QUIT)
    public Response quit (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          String id) {
        final GamePlayer found = gamesMaster.findPlayer(room, id);
        if (found == null) return notFound(id);

        gamesMaster.removePlayer(room, id);
        return ok();
    }

}
