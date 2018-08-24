package wordland.model.game;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.Await;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import wordland.model.GameBoard;
import wordland.model.GameBoardBlock;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.cobbzilla.util.daemon.DaemonThreadFactory.fixedPool;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;
import static wordland.ApiConstants.MAX_BOARD_VIEW;
import static wordland.model.GameBoardBlock.getBlockKeyForTile;
import static wordland.model.support.GameNotification.invalidWord;
import static wordland.model.support.GameNotification.sparseWord;
import static wordland.model.support.PlayedTile.letterFarFromOthers;

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

    public GameBoardState getBoard(int x1, int x2, int y1, int y2) {

        final GameBoard board = roomSettings.getBoard();
        final GameBoardSettings settings = board.getSettings();

        if (settings.hasLength()) {
            if (x1 < 0) x1 = 0;
            if (settings.getLength() > x2) x2 = settings.getLength();
        }
        if (x2-x1 >= MAX_BOARD_VIEW) {
            x2 = x1 + MAX_BOARD_VIEW - 1;
        }
        if (settings.hasWidth()) {
            if (y1 < 0) y1 = 0;
            if (settings.getWidth() > y2) y2 = settings.getLength();
        }
        if (y2-y1 >= MAX_BOARD_VIEW) {
            y2 = y1 + MAX_BOARD_VIEW - 1;
        }

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Map<String,  GameBoardBlock> blockMap = new HashMap<>();
        final GameBoardState boardState;
        synchronized (stateStorage) {
            boardState = new GameBoardState(stateStorage.getVersion(), x1, x2, y1, y2);
            for (String blockKey : blockKeys) {
                final GameBoardBlock block = stateStorage.getBlockOrCreate(blockKey, roomSettings.getSymbolDistribution());
                blockMap.put(block.getBlockKey(), block);
            }
        }

        final long start = now();
        final GameTileState[][] tiles = new GameTileState[Math.abs(x2-x1)+1][Math.abs(y2-y1)+1];
        final Collection<Future<?>> futures = new ArrayList<>();
        @Cleanup("shutdownNow") final ExecutorService pool = fixedPool(100);
        final int X1 = x1;
        final int Y1 = y1;
        for (int x=0; x<tiles.length; x++) {
            for (int y=0; y<tiles[x].length; y++) {
                final int thisX = x;
                final int thisY = y;
                futures.add(pool.submit(() -> {
                    final String blockKeyForTile = getBlockKeyForTile(X1 + thisX, Y1 + thisY);
                    final GameBoardBlock block = blockMap.get(blockKeyForTile);
                    if (block == null) {
                        log.error("getBoard: no block found for key: "+blockKeyForTile);
                    } else {
                        tiles[thisX][thisY] = block.getAbsoluteTile(X1 + thisX, Y1 + thisY);
                    }
                }));
            }
        }
        Await.awaitAll(futures, 10*TimeUtil.SECOND);
        final String duration = formatDuration(now() - start);
        log.info("mapping of blocks took "+ duration);

        return boardState.setTiles(tiles);
    }

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {

        if (word.length() < 2) {
            throw new GameNotificationException(invalidWord(word));
        }
        if (!roomSettings.getDictionary().isWord(word)) {
            throw new GameNotificationException(invalidWord(word));
        }
        if (roomSettings.hasMaxLetterDistance()) {
            final PlayedTile farTile = letterFarFromOthers(tiles, roomSettings.getMaxLetterDistance());
            if (farTile != null) {
                throw new GameNotificationException(sparseWord(word, roomSettings.getMaxLetterDistance(), farTile));
            }
        }

        final Map<String, GameBoardBlock> alteredBlocks = new HashMap<>();
        synchronized (stateStorage) {
            // determine applicable board blocks, create change set
            for (int i=0; i<tiles.length; i++) {
                final PlayedTile tile = tiles[i];
                final String blockKey = getBlockKeyForTile(tile.getX(), tile.getY());
                final GameBoardBlock block = alteredBlocks.computeIfAbsent(blockKey, (k) -> stateStorage.getBlock(blockKey));
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
        final GameBoardBlock block = stateStorage.getBlockOrCreate(getBlockKeyForTile(x, y), roomSettings.getSymbolDistribution());
        return block.getAbsoluteTile(x, y);
    }
}
