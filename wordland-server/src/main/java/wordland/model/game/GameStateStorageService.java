package wordland.model.game;

import wordland.model.GameBoardBlock;
import wordland.model.SymbolDistribution;
import wordland.model.support.PlayedTile;

import java.util.Collection;

public interface GameStateStorageService {

    long getVersion();

    RoomState getRoomState();
    void startGame();
    void endGame();

    GamePlayer getPlayer(String id);
    int getPlayerCount();

    GameStateChange addPlayer(GamePlayer player);
    GameStateChange addPlayerStartGame(GamePlayer player);

    GameStateChange removePlayer(String id);
    GameStateChange removePlayerEndGame(String id);

    GameBoardBlock getBlock(String blockKey);
    GameBoardBlock getBlockOrCreate(String blockKey, SymbolDistribution distribution);

    GameStateChange playWord(GamePlayer player,
                             Collection<GameBoardBlock> blocks,
                             PlayedTile[] tiles);

    String getCurrentPlayerId();
}
