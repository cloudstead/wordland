package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.*;
import wordland.model.GameRoom;
import wordland.model.game.*;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.*;
import wordland.service.GamesMaster;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static org.cobbzilla.util.http.HttpContentTypes.TEXT_PLAIN;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static wordland.ApiConstants.*;
import static wordland.model.GameBoardBlock.BLOCK_SIZE;
import static wordland.model.game.GameBoardPalette.defaultPalette;
import static wordland.model.game.GamePlayer.UNKNOWN_PLAYER;
import static wordland.model.support.GameRuntimeEvent.disableTilesJson;

@Path(GAME_ROOMS_ENDPOINT)
@Service @Slf4j
public class GameRoomsResource extends NamedSystemResource<GameRoom> {

    @Autowired @Getter private GameRoomDAO dao;
    @Autowired @Getter private GameBoardDAO gameBoardDAO;
    @Autowired @Getter private GamesMaster gamesMaster;
    @Autowired @Getter private SymbolSetDAO symbolSetDAO;
    @Autowired @Getter private SymbolDistributionDAO distributionDAO;
    @Autowired @Getter private PointSystemDAO pointSystemDAO;
    @Autowired @Getter private GameDictionaryDAO dictionaryDAO;

    @Override protected boolean canCreate(HttpContext ctx) { return true; }

    @Override public Response findAll(HttpContext ctx) { return ok(dao.findTemplates()); }

    @PUT
    public Response create (@Context HttpContext ctx,
                            @Valid GameRoom gameRoom) {
        return create(ctx, gameRoom.getName(), gameRoom);
    }

    @PUT @Path("/{room}")
    public Response create (@Context HttpContext ctx,
                            @PathParam("room") String room,
                            @Valid GameRoom gameRoom) {

        final AccountSession account = accountPrincipal(ctx);
        final GameRoom existing = dao.findByName(room);
        if (existing != null) return invalid("err.room.exists");

        final GameRoomSettings rs = gameRoom.getSettings();
        dao.setDefaults(rs);
        if (account != null) gameRoom.setAccountOwner(account.getUuid());

        return super.create(ctx, gameRoom);
    }

    @POST @Path("/{room}"+EP_JOIN)
    public Response join (@Context HttpContext ctx,
                          @PathParam("room") String room,
                          @Valid GameRoomJoinRequest request) {

        final AccountSession session = userPrincipal(ctx);
        final GamePlayer player = new GamePlayer(session, request);

        final GameRoom gameRoom = dao.findByName(room);
        if (gameRoom == null) return notFound(room);

        return ok(gamesMaster.addPlayer(gameRoom, player));
    }

    @GET @Path("/{name}"+EP_PLAYERS+"/{id}")
    public Response playerInfo (@Context HttpContext ctx,
                                @PathParam("name") String room,
                                @PathParam("id") String playerId) {
        final AccountSession session = userPrincipal(ctx);
        final GameRoom gameRoom = dao.findByName(room);
        if (gameRoom == null) return notFound(room);

        final GamePlayer player = gamesMaster.findCurrentOrFormerPlayer(room, playerId);
        return player == null ? ok(UNKNOWN_PLAYER) : ok(player);
    }

    @POST @Path("/{room}"+EP_ABANDON)
    public Response abandon (@Context HttpContext ctx,
                             @PathParam("room") String room) {
        final AccountSession session = userPrincipal(ctx);

        final GameRoom gameRoom = dao.findByName(room);
        if (gameRoom == null) return notFound(room);

        final GameStateChange change = gamesMaster.removePlayer(gameRoom.getName(), session.getApiToken(), session.getId());
        return change == null ? ok() : ok(change);
    }

    @GET @Path("/{name}"+EP_BOARD)
    public Response board (@Context HttpContext ctx,
                           @PathParam("name") String room,
                           @QueryParam("x1") Integer x1,
                           @QueryParam("x2") Integer x2,
                           @QueryParam("y1") Integer y1,
                           @QueryParam("y2") Integer y2) {
        return ok(getGameBoardState(room, x1, x2, y1, y2));
    }

    protected GameBoardState getGameBoardState(@PathParam("name") String room, @QueryParam("x1") Integer x1, @QueryParam("x2") Integer x2, @QueryParam("y1") Integer y1, @QueryParam("y2") Integer y2) {
        final GameState state = gamesMaster.getGameState(room);
        if (state == null) throw notFoundEx(room);

        if (x1 == null) x1 = 0;
        if (x2 == null) x2 = BLOCK_SIZE-1;
        if (y1 == null) y1 = 0;
        if (y2 == null) y2 = BLOCK_SIZE-1;

        return state.getBoard(x1, x2, y1, y2)
                .setRoomState(state.getRoomState())
                .setWinners(state.hasWinners()
                        ? state.getWinners().toArray(new String[0])
                        : null);
    }

    @POST @Path("/{name}"+EP_PLAY)
    public Response play (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          @Valid GameRuntimeEvent request) {

        final AccountSession account = userPrincipal(ctx);
        if (!request.getId().equals(account.getId())) return forbidden();
        if (!request.hasWord()) return invalid("err.word.required");

        // if the player cannot be found, they might have left the game
        // return an appropriate game exit status
        final GamePlayer player = gamesMaster.findPlayer(room, request.getId());
        if (player == null) {
            final GamePlayerExitStatus exitStatus = gamesMaster.findPlayerStatus(room, request.getId());
            if (exitStatus == null) return notFound(request.getId());
            return invalid("err.game."+exitStatus.name());
        }

        final GameStateChange played = gamesMaster.playWord(room, request.getApiToken(), player, request.getWord(), request.getTiles());
        return ok(played);
    }

    @GET @Path("/{name}"+EP_PLAYS)
    public Response play (@Context HttpContext ctx,
                          @PathParam("name") String room) {
        final GameState state = gamesMaster.getGameState(room);
        if (state == null) throw notFoundEx(room);
        return ok(disableTilesJson(state.getPlays()));
    }

    @POST @Path("/{name}"+EP_QUIT)
    public Response quit (@Context HttpContext ctx,
                          @PathParam("name") String room,
                          @Valid GameRuntimeEvent request) {

        final AccountSession account = userPrincipal(ctx);
        if (!request.getId().equals(account.getId())) return forbidden();
        if (request.getStateChange() != GameStateChangeType.player_left) return invalid("err.type.invalid", request.getStateChange().name());

        final GamePlayer found = gamesMaster.findPlayer(room, request.getId());
        if (found == null || !found.getApiToken().equals(account.getApiToken())) return notFound(request.getId());

        gamesMaster.removePlayer(room, request.getApiToken(), found.getId());
        return ok();
    }

    @GET @Path("/{name}"+EP_SCOREBOARD)
    public Response getScoreboard(@Context HttpContext ctx,
                                  @PathParam("name") String room) {
        final AccountSession account = userPrincipal(ctx);
        return ok(getScoreboard(room));
    }

    protected List<ScoreboardEntry> getScoreboard(@PathParam("name") String room) {
        final GameState state = gamesMaster.getGameState(room);
        final Map<String, GamePlayer> players = state.getCurrentAndFormerPlayers();
        final Map<String, Integer> scoreboard = state.getScoreboard();
        final List<ScoreboardEntry> scoreboardList = new ArrayList<>();
        final Collection<String> winners = state.getWinners();
        for (Map.Entry<String, Integer> entry : scoreboard.entrySet()) {
            final GamePlayer player = players.get(entry.getKey());
            if (player != null) {
                scoreboardList.add(new ScoreboardEntry()
                        .setId(player.getId())
                        .setName(player.getName())
                        .setScore(entry.getValue())
                        .checkWinner(winners));
            } else {
                // should never happen
                scoreboardList.add(new ScoreboardEntry()
                        .setId(entry.getKey())
                        .setName(entry.getKey())
                        .setScore(entry.getValue())
                        .checkWinner(winners));
            }
        }
        Collections.sort(scoreboardList);
        return scoreboardList;
    }

    @GET @Path("/{name}"+EP_BOARD+EP_VIEW_PNG)
    public Response boardImageView(@Context HttpContext ctx,
                                   @PathParam("name") String room,
                                   @QueryParam("x1") Integer x1,
                                   @QueryParam("x2") Integer x2,
                                   @QueryParam("y1") Integer y1,
                                   @QueryParam("y2") Integer y2,
                                   @QueryParam("width") Integer width,
                                   @QueryParam("height") Integer height,
                                   @QueryParam("noCache") Boolean noCache,
                                   @QueryParam("palette") String paletteJson) {
        final GameBoardPalette palette = paletteJson != null
                ? json(paletteJson, GameBoardPalette.class)
                : null;
        return boardImageView(ctx, room, x1, x2, y1, y2, width, height, noCache != null && noCache, palette);
    }

    @POST @Path("/{name}"+EP_BOARD+EP_VIEW_PNG)
    public Response boardImageView(@Context HttpContext ctx,
                                   @PathParam("name") String room,
                                   @QueryParam("x1") Integer x1,
                                   @QueryParam("x2") Integer x2,
                                   @QueryParam("y1") Integer y1,
                                   @QueryParam("y2") Integer y2,
                                   @QueryParam("width") Integer width,
                                   @QueryParam("height") Integer height,
                                   @QueryParam("noCache") Boolean noCache,
                                   GameBoardPalette palette) {

        final AccountSession account = userPrincipal(ctx);
        final GameState state = gamesMaster.getGameState(room);
        if (state == null) return notFound(room);

        if (x1 == null) x1 = 0;
        if (x2 == null) x2 = (BLOCK_SIZE*10)-1;
        if (y1 == null) y1 = 0;
        if (y2 == null) y2 = (BLOCK_SIZE*10)-1;
        if (height == null) height = 300;
        if (width == null) width = 400;

        if (palette == null) palette = defaultPalette(account.getId());

        try {
            return ok(state.getBoardView(x1, x2, y1, y2, width, height, palette, noCache != null && noCache));
        } catch (IOException e) {
            return invalid("err.boardImageView.rendering");
        }
    }

    @GET @Path("/{name}"+EP_BOARD+EP_VIEW_TXT)
    @Produces(TEXT_PLAIN)
    public Response boardTextView (@Context HttpContext ctx,
                                   @PathParam("name") String room,
                                   @QueryParam("x1") Integer x1,
                                   @QueryParam("x2") Integer x2,
                                   @QueryParam("y1") Integer y1,
                                   @QueryParam("y2") Integer y2,
                                   @QueryParam("sb") Boolean includeScoreboard,
                                   @QueryParam("palette") String paletteJson) {
        final GameBoardPalette palette = paletteJson != null
                ? json(paletteJson, GameBoardPalette.class)
                : null;
        return boardTextView(ctx, room, x1, x2, y1, y2, includeScoreboard, palette);
    }

    @POST @Path("/{name}"+EP_BOARD+EP_VIEW_TXT)
    @Produces(TEXT_PLAIN)
    public Response boardTextView (@Context HttpContext ctx,
                                   @PathParam("name") String room,
                                   @QueryParam("x1") Integer x1,
                                   @QueryParam("x2") Integer x2,
                                   @QueryParam("y1") Integer y1,
                                   @QueryParam("y2") Integer y2,
                                   @QueryParam("sb") Boolean includeScoreboard,
                                   GameBoardPalette palette) {

        final AccountSession account = userPrincipal(ctx);
        if (palette == null) palette = defaultPalette(account.getId());
        palette.setAnsi();
        if (includeScoreboard == null || !includeScoreboard) palette.setScoreboard(getScoreboard(room));

        final GameBoardState board = getGameBoardState(room, x1, x2, y1, y2);
        return ok(board.grid(palette));
    }

    @POST @Path("/{name}"+EP_BOARD+EP_PREVIEW_TXT)
    public Response boardTextPreview (@Context HttpContext ctx,
                                      @PathParam("name") String room,
                                      @QueryParam("x1") Integer x1,
                                      @QueryParam("x2") Integer x2,
                                      @QueryParam("y1") Integer y1,
                                      @QueryParam("y2") Integer y2,
                                      @QueryParam("sb") Boolean includeScoreboard,
                                      TextPreviewRequest request) {

        final AccountSession account = userPrincipal(ctx);

        // prep request, palette and tiles
        if (request == null) request = new TextPreviewRequest();
        if (!request.hasPalette()) request.setPalette(defaultPalette(account.getId()));
        request.getPalette().setAnsi();
        if (includeScoreboard == null || !includeScoreboard) request.getPalette().setScoreboard(getScoreboard(room));

        if (request.hasTiles()) {
            if (!request.isValid()) return invalid("err.attempt.invalid");
            for (AttemptedTile a : request.getTiles()) a.setOwner(account.getUuid());
        }

        final GameBoardState board = getGameBoardState(room, x1, x2, y1, y2);
        final TextGridResponse text = board.grid(request.getPalette(), request.getTiles())
                .setPalette(request.getPalette())
                .setRoomState(board.getRoomState())
                .setScoreboard(getScoreboard(room).toArray(new ScoreboardEntry[0]));
        return ok(text);
    }

    @GET @Path("/{name}"+EP_SETTINGS)
    public Response settings (@Context HttpContext ctx,
                              @PathParam("name") String roomName) {
        final GameRoom room = gamesMaster.findRoom(roomName);
        return room == null ? notFound(roomName) : ok(room.getSettings());
    }

}
