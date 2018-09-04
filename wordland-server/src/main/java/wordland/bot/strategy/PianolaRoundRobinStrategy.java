package wordland.bot.strategy;

import wordland.bot.PianolaBot;
import wordland.model.game.GameStateStorageService;

public class PianolaRoundRobinStrategy extends PianolaBotBase {

    public PianolaRoundRobinStrategy(PianolaBot pianolaBot) { super(pianolaBot); }

    @Override protected boolean shouldPlayTurn() {
        final GameStateStorageService stateStorage = getStateStorage();
        final String currentPlayerId = stateStorage.getCurrentPlayerId();
        return myPlayerId().equals(currentPlayerId);
    }

}
