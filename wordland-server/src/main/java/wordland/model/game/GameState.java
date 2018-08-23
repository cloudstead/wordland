package wordland.model.game;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import wordland.model.GameBoard;
import wordland.model.GameBoardBlock;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static wordland.model.GameBoardBlock.getBlockKeyForTile;

@Slf4j
public class GameState {

    private final GameRoomSettings roomSettings;
    private final GameStateStorageService stateStorage;

    public GameState(GameRoomSettings roomSettings, GameStateStorageService stateStorage) {
        this.roomSettings = roomSettings;
        this.stateStorage = stateStorage;
    }

    public GamePlayer getPlayer(String id) { return stateStorage.getPlayer(id); }

    public GameStateChange addPlayer(GamePlayer player) {
        // todo check maxPlayers. if maxPlayers reached, see if any can be evicted? maybe not, let GameDaemon handle that...
        if (stateStorage.getPlayerCount() >= roomSettings.getMaxPlayers()) throw new SimpleViolationException("err.maxPlayersInRoom", "maximum "+roomSettings.getMaxPlayers()+" players allowed in room", ""+roomSettings.getMaxPlayers());
        return stateStorage.addPlayer(player);
    }

    public GameStateChange removePlayer(String id) {
        return stateStorage.removePlayer(id);
    }

    public GameTileState[][] getBoard(int x1, int x2, int y1, int y2) {

        final GameBoard board = roomSettings.getBoard();
        final GameBoardSettings settings = board.getSettings();

        if (settings.hasLength()) {
            if (x1 < 0) x1 = 0;
            if (settings.getLength() > x2) x2 = settings.getLength();
        }
        if (settings.hasWidth()) {
            if (y1 < 0) y1 = 0;
            if (settings.getWidth() > y2) y2 = settings.getLength();
        }

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Map<String,  GameBoardBlock> blockMap = new HashMap<>();
        for (String blockKey : blockKeys) {
            final GameBoardBlock block = stateStorage.newBlock(blockKey, roomSettings.getSymbolDistribution());
            blockMap.put(block.getBlockKey(), block);
        }

        final GameTileState[][] tiles = new GameTileState[x2-x1][y2-y1];
        for (int x=x1; x<x2; x++) {
            for (int y=y1; y<y2; y++) {
                final GameBoardBlock block = blockMap.get(getBlockKeyForTile(x, y));
                tiles[x-x1][y-y1] = block.getTiles()[x][y];
            }
        }

        return tiles;
    }

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {
        synchronized (stateStorage) {
            // determine applicable board blocks, create change set
            final Map<String, GameBoardBlock> alteredBlocks = new HashMap<>();
            for (int i = 0; i < tiles.length; i++) {
                final PlayedTile tile = tiles[i];
                final GameBoardBlock block = stateStorage.getBlock(GameBoardBlock.getBlockKeyForTile(tile.getX(), tile.getY()));
                if (block == null) return die("playWord: uninitialized block");

                final GameTileState boardTile = block.getTiles()[tile.getX() - block.getX1()][tile.getY() - block.getY1()];
                if (!boardTile.getSymbol().equals(tile.getSymbol())) {
                    die("playWord: invalid play");
                }
                if (isClaimableByPlayer(player, boardTile, tile.getX(), tile.getY())) {
                    boardTile.setOwner(player.getId());
                    alteredBlocks.put(block.getBlockKey(), block);
                }
            }
            return stateStorage.playWord(player, alteredBlocks.values(), tiles);
        }
    }

    private boolean isClaimableByPlayer(GamePlayer player, GameTileState boardTile, int x, int y) {
        if (!boardTile.hasOwner()) return true;
        final String currentOwner = boardTile.getOwner();
        if (currentOwner.equals(player.getId())) return true;
        GameTileState adjacent;
        final GameBoardSettings boardSettings = roomSettings.getBoard().getSettings();
        if (boardSettings.infiniteLength() || x > 0) {
            adjacent = getTileState(x-1, y);
            if (!adjacent.hasOwner() || !adjacent.getOwner().equals(currentOwner)) return true;
        }
        if (boardSettings.infiniteLength() || x < boardSettings.getLength() -1) {
            adjacent = getTileState(x+1, y);
            if (!adjacent.hasOwner() || !adjacent.getOwner().equals(currentOwner)) return true;
        }
        if (boardSettings.infiniteWidth() || y > 0) {
            adjacent = getTileState(x, y-1);
            if (!adjacent.hasOwner() || !adjacent.getOwner().equals(currentOwner)) return true;
        }
        if (boardSettings.infiniteWidth() || y < boardSettings.getWidth()-1) {
            adjacent = getTileState(x, y+1);
            if (!adjacent.hasOwner() || !adjacent.getOwner().equals(currentOwner)) return true;
        }
        return false;
    }

    private GameTileState getTileState (int x, int y) {
        final GameBoardBlock block = stateStorage.getBlock(GameBoardBlock.getBlockKeyForTile(x, y));
        return block.getAbsoluteTile(x, y);
    }
}
