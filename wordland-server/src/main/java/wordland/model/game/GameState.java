package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class GameState {

    @Getter private int version = 0;
    @Getter private int length;
    @Getter private int width;
    @Getter private int maxPlayers;
    @Getter private GameTileState[][] tiles;
    @Getter private Map<String, GamePlayer> players;

    public GameState(int length, int width, int maxPlayers) {
        this.length = length;
        this.width = width;
        this.maxPlayers = maxPlayers;
        tiles = new GameTileState[length][width];
        for (int x=0; x<length; x++) {
            for (int y=0; y<width; y++) {
                tiles[x][y] = new GameTileState();
            }
        }
        players = new ConcurrentHashMap<>(maxPlayers);
    }

    public GameState(GameState other) {
        length = other.length;
        width = other.width;
        maxPlayers = other.maxPlayers;
        tiles = new GameTileState[length][];
        for (int x=0; x<length; x++) {
            tiles[x] = new GameTileState[width];
            System.arraycopy(other.tiles[x], 0, tiles[x], 0, width);
        }
        players = new HashMap<>(other.players);
    }

    public void setTileSymbol(int x, int y, String symbol) { tiles[x][y].setSymbol(symbol); }

    public GamePlayer getPlayer(String account) { return players.get(account); }

    public void addPlayer(GamePlayer player) {
        // todo check maxPlayers. if maxPlayers reached, see if any can be evicted? maybe not, let GameDaemon handle that...
        players.put(player.getId(), player);
    }

    public void removePlayer(String uuid) { players.remove(uuid); }

}
