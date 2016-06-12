package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import wordland.model.support.PlayedTile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor
public class GameState {

    @Getter private AtomicInteger version = new AtomicInteger(0);
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

    public GameStateChange addPlayer(GamePlayer player) {
        // todo check maxPlayers. if maxPlayers reached, see if any can be evicted? maybe not, let GameDaemon handle that...
        players.put(player.getId(), player);
        return GameStateChange.playerJoined(version.incrementAndGet(), player);
    }

    public GameStateChange removePlayer(String id) {
        players.remove(id);
        return GameStateChange.playerLeft(version.incrementAndGet(), id);
    }

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {
        final GameTileState[] boardTiles = new GameTileState[tiles.length];
        final StringBuilder b = new StringBuilder();
        for (int i=0; i<tiles.length; i++) {
            final PlayedTile tile = tiles[i];
            final GameTileState boardTile = this.tiles[tile.getX()][tile.getY()];
            if (!boardTile.getSymbol().equals(tile.getSymbol())) {
                die("playWord: invalid play");
            }
            boardTiles[i] = boardTile;
            b.append(tile.getSymbol());
        }

        if (!b.toString().equalsIgnoreCase(word)) die("playWord: word does not match tiles");

        for (int i=0; i<boardTiles.length; i++) {
            boardTiles[i].setOwner(player.getId());
        }

        return GameStateChange.wordPlayed(version.incrementAndGet(), player, tiles);
    }
}
