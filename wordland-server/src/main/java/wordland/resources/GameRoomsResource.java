package wordland.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameRoomDAO;
import wordland.model.GameRoom;

import javax.ws.rs.Path;

import static wordland.ApiConstants.GAME_ROOMS_ENDPOINT;

@Path(GAME_ROOMS_ENDPOINT)
@Service @Slf4j
public class GameRoomsResource extends NamedSystemResource<GameRoom> {

    @Getter @Autowired private GameRoomDAO dao;

}
