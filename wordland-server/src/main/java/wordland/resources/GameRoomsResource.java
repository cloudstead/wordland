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
import wordland.model.game.GameState;
import wordland.model.game.GameStateChangeType;
import wordland.model.support.GameRoomJoinRequest;
import wordland.model.support.GameRuntimeEvent;
import wordland.service.GamesMaster;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static wordland.ApiConstants.*;

@Path(GAME_ROOMS_ENDPOINT)
@Service @Slf4j
public class GameRoomsResource extends NamedSystemResource<GameRoom> {

    @Getter @Autowired private GameRoomDAO dao;
    @Getter @Autowired private GamesMaster gamesMaster;

    @PUT
    public Response create (@Context HttpContext ctx,
                            @Valid GameRoom gameRoom) {
        return create(ctx, gameRoom.getName(), gameRoom);
    }

    @PUT @Path("/{name}")
    public Response create (@Context HttpContext ctx,
                            @PathParam("name") String room,
                            @Valid GameRoom gameRoom) {
        final Account account = userPrincipal(ctx);
        gamesMaster.newRoom(gameRoom);
        return ok(gameRoom);
    }

    @POST @Path("/{name}"+EP_JOIN)
    public Response join (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          @Valid GameRoomJoinRequest request) {

        final Account account = optionalUserPrincipal(ctx);
        final GamePlayer player = new GamePlayer(account, request);

        final GamePlayer found = gamesMaster.findPlayer(room, player);
        if (found != null) return ok(found);

        gamesMaster.addPlayer(room, player);
        return ok(player.getCredentials());
    }

    @POST @Path("/{name}"+EP_QUIT)
    public Response quit (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          @Valid GameRuntimeEvent request) {

        if (request.getStateChange() != GameStateChangeType.player_left) return invalid("err.type.invalid", request.getStateChange().name());

        final GamePlayer found = gamesMaster.findPlayer(room, request.getId());
        if (found == null) return notFound(request.getId());
        if (!found.getApiKey().equals(request.getApiKey())) return forbidden();

        gamesMaster.removePlayer(room, found.getId());
        return ok();
    }

    @GET @Path("/{name}"+EP_STATE)
    public Response state (@Context HttpContext ctx,
                           @PathParam("name") String room) {
        final GameState state = gamesMaster.getGameState(room);
        return state == null ? notFound(room) : ok(state);
    }

    @GET @Path("/{name}"+EP_SETTINGS)
    public Response settings (@Context HttpContext ctx,
                              @PathParam("name") String roomName) {
        final GameRoom room = gamesMaster.findRoom(roomName);
        return room == null ? notFound(roomName) : ok(room.getSettings());
    }

}
