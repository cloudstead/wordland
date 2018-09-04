package wordland.model.game;

import org.cobbzilla.wizard.validation.SimpleViolationException;
import wordland.model.GameRoom;

public class RoomFullException extends SimpleViolationException  {

    public RoomFullException(GameRoom room) {
        super("err.game.maxPlayers",
              "maximum "+room.getSettings().getMaxPlayers()+" players already in room "+room.getName(),
              String.valueOf(room.getSettings().getMaxPlayers()));
    }

}
