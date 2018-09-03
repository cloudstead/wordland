package wordland.bot;

import wordland.model.game.GamePlayer;
import wordland.model.game.GameTileState;

public interface PianolaBrain {

    PianolaPlay play(GamePlayer player, GameTileState[][] tiles, int x, int y) throws Exception;

}
