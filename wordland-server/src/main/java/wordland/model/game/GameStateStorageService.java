package wordland.model.game;

import wordland.model.GameBoardBlock;
import wordland.model.SymbolDistribution;
import wordland.model.support.PlayedTile;

import java.util.Collection;

public interface GameStateStorageService {

    GamePlayer getPlayer(String id);
    int getPlayerCount();
    GameStateChange addPlayer(GamePlayer player);
    GameStateChange removePlayer(String id);

    GameBoardBlock getBlock(String blockKey);
    GameBoardBlock newBlock(String blockKey, SymbolDistribution defaultDistribution);

    GameStateChange playWord(GamePlayer player,
                             Collection<GameBoardBlock> blocks,
                             PlayedTile[] tiles);
}