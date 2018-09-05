package wordland.model.game;

import org.cobbzilla.wizard.validation.SimpleViolationException;
import wordland.model.GameRoom;

public class GameEndedException extends SimpleViolationException {

    public GameEndedException(GameRoom room) {
        super("err.game.gameOver", "Game has ended in room "+room.getName());
    }

}
