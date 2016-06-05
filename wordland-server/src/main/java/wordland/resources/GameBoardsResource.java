package wordland.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameBoardDAO;
import wordland.model.GameBoard;

import javax.ws.rs.Path;

import static wordland.ApiConstants.GAME_BOARDS_ENDPOINT;

@Path(GAME_BOARDS_ENDPOINT)
@Service @Slf4j
public class GameBoardsResource extends NamedSystemResource<GameBoard> {

    @Getter @Autowired private GameBoardDAO dao;

}
