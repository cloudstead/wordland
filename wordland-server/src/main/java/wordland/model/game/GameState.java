package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class GameState {

    @Getter private GameTileState[][] tiles;
    @Getter private Map<String, GamePlayer> players;

    public GameState(int length, int width, int maxPlayers) {
        tiles = new GameTileState[length][width];
        for (int x=0; x<length; x++) {
            for (int y=0; y<width; y++) {
                tiles[x][y] = new GameTileState();
            }
        }
        players = new ConcurrentHashMap<>(maxPlayers);
    }

    public void setTileSymbol(int x, int y, String symbol) { tiles[x][y].setSymbol(symbol); }

    public GamePlayer getPlayer(String account) { return players.get(account); }

    public void addPlayer(GamePlayer player) { players.put(player.getId(), player); }

    public void removePlayer(String uuid) { players.remove(uuid); }

}
