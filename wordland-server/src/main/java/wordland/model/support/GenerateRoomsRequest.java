package wordland.model.support;

import wordland.model.*;
import wordland.model.json.GameRoomSettings;

import java.util.Iterator;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.util.TestNames.color;
import static org.cobbzilla.wizard.util.TestNames.safeAnimal;

public class GenerateRoomsRequest {

    public SymbolSet[] symbolSets;
    public Integer[] maxPlayers;
    public Integer[] maxLetterDistances;
    public TeamPlayMode[] teamPlayModes;
    public TurnPolicy[][] turnPolicies;
    public MissedTurnPolicy[] missedTurnPolicies;
    public BonusPolicy[][] bonusPolicies;
    public WinCondition[][] winConditions;

    public Iterator<GameRoom> getGenerator(GameBoard board, GameRoomSettings defaults) {
        return new GameRoomIterator(board, defaults);
    }

    private class GameRoomIterator implements Iterator<GameRoom> {

        private GameBoard board;
        private GameRoomSettings defaults;
        private GameRoom next;
        private int index = 0;
        private int max = (empty(symbolSets) ? 1 : symbolSets.length)
                * (empty(maxPlayers) ? 1 : maxPlayers.length)
                * (empty(maxLetterDistances) ? 1 : maxLetterDistances.length)
                * (empty(teamPlayModes) ? 1 : teamPlayModes.length)
                * (empty(turnPolicies) ? 1 : turnPolicies.length)
                * (empty(missedTurnPolicies) ? 1 : missedTurnPolicies.length)
                * (empty(bonusPolicies) ? 1 : bonusPolicies.length)
                * (empty(winConditions) ? 1 : winConditions.length);

        public GameRoomIterator(GameBoard board, GameRoomSettings defaults) {
            this.board = board;
            this.defaults = defaults;
            this.next = getNextRoom();
        }

        @Override public boolean hasNext() { return next != null; }

        @Override public GameRoom next() {
            final GameRoom toReturn = next;
            next = getNextRoom();
            return toReturn;
        }

        private GameRoom getNextRoom() {
            if (index >= max) return null;
            final SymbolSet symbolSet = empty(symbolSets) ? defaults.getSymbolSet() : symbolSets[index % symbolSets.length];
            final GameRoomSettings rs = new GameRoomSettings()
                    .setBoard(board)
                    .setSymbolSet(symbolSet)
                    .setSymbolDistribution(symbolSet.hasChildren(SymbolDistribution.class) ? symbolSet.getChildren(SymbolDistribution.class).get(0) : defaults.getSymbolDistribution())
                    .setPointSystem(symbolSet.hasChildren(PointSystem.class) ? symbolSet.getChildren(PointSystem.class).get(0) : defaults.getPointSystem())
                    .setDictionary(symbolSet.hasChildren(GameDictionary.class) ? symbolSet.getChildren(GameDictionary.class).get(0) : defaults.getDictionary())
                    .setMaxPlayers(empty(maxPlayers) ? defaults.getMaxPlayers() : maxPlayers[index % maxPlayers.length])
                    .setMaxLetterDistance(empty(maxLetterDistances) ? defaults.getMaxLetterDistance() : maxLetterDistances[index % maxLetterDistances.length])
                    .setTeamPlayMode(empty(teamPlayModes) ? defaults.getTeamPlayMode() : teamPlayModes[index % teamPlayModes.length])
                    .setMissedTurnPolicy(empty(missedTurnPolicies) ? defaults.getMissedTurnPolicy() : missedTurnPolicies[index % missedTurnPolicies.length])
                    .setBonusPolicies(empty(bonusPolicies) ? defaults.getBonusPolicies() : bonusPolicies[index % bonusPolicies.length])
                    .setWinConditions(empty(winConditions) ? defaults.getWinConditions() : winConditions[index % winConditions.length])
                    ;
            index++;
            return new GameRoom(color()+"_"+safeAnimal()).setSettings(rs).setTemplate(true);
        }
    }
}
