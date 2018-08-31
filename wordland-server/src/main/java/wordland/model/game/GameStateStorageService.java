package wordland.model.game;

import wordland.model.GameBoardBlock;
import wordland.model.SymbolDistribution;
import wordland.model.game.score.PlayScore;
import wordland.model.support.PlayedTile;

import java.util.Collection;
import java.util.Map;

public interface GameStateStorageService {

    long getVersion();

    RoomState getRoomState();
    void startGame();
    void endGame();

    GamePlayer getPlayer(String id);
    Collection<GamePlayer> getPlayers();
    int getPlayerCount();

    GameStateChange addPlayer(GamePlayer player);
    GameStateChange addPlayerStartGame(GamePlayer player);

    GameStateChange removePlayer(String id);
    GameStateChange removePlayerEndGame(String id);

    GameBoardBlock getBlock(String blockKey);
    GameBoardBlock getBlockOrCreate(String blockKey, SymbolDistribution distribution);

    GameStateChange playWord(GamePlayer player,
                             Collection<GameBoardBlock> blocks,
                             PlayedTile[] tiles,
                             PlayScore score);

    String getCurrentPlayerId();

    Map<String, String> getScorebord ();

}
