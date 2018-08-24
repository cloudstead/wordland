package wordland.model.game;

import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.Await;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import wordland.model.GameBoard;
import wordland.model.GameBoardBlock;
import wordland.model.GameRoom;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.DaemonThreadFactory.fixedPool;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;
import static wordland.ApiConstants.MAX_BOARD_DETAIL_VIEW;
import static wordland.ApiConstants.MAX_BOARD_VIEW;
import static wordland.model.GameBoardBlock.getBlockKeyForTile;
import static wordland.model.support.GameNotification.invalidWord;
import static wordland.model.support.GameNotification.sparseWord;
import static wordland.model.support.PlayedTile.letterFarFromOthers;

@Slf4j
public class GameState {

    private final GameRoom room;
    private final GameStateStorageService stateStorage;

    public GameState(GameRoom room, GameStateStorageService stateStorage) {
        this.room = room;
        this.stateStorage = stateStorage;
    }

    private GameRoomSettings roomSettings() { return room.getSettings(); }

    public GamePlayer getPlayer(String id) { return stateStorage.getPlayer(id); }

    public GameStateChange addPlayer(GamePlayer player) {
        // todo check maxPlayers. if maxPlayers reached, see if any can be evicted? maybe not, let GameDaemon handle that...
        if (stateStorage.getPlayerCount() >= roomSettings().getMaxPlayers()) throw new SimpleViolationException("err.maxPlayersInRoom", "maximum "+roomSettings().getMaxPlayers()+" players allowed in room", ""+roomSettings().getMaxPlayers());
        return stateStorage.addPlayer(player);
    }

    public GameStateChange removePlayer(String id) {
        return stateStorage.removePlayer(id);
    }

    public GameBoardState getBoard(int x1, int x2, int y1, int y2) {

        final GameBoard board = roomSettings().getBoard();
        final GameBoardSettings settings = board.getSettings();

        if (settings.hasLength()) {
            if (x1 < 0) x1 = 0;
            if (settings.getLength() > x2) x2 = settings.getLength();
        }
        if (x2-x1 >= MAX_BOARD_DETAIL_VIEW) {
            x2 = x1 + MAX_BOARD_DETAIL_VIEW - 1;
        }
        if (settings.hasWidth()) {
            if (y1 < 0) y1 = 0;
            if (settings.getWidth() > y2) y2 = settings.getLength();
        }
        if (y2-y1 >= MAX_BOARD_DETAIL_VIEW) {
            y2 = y1 + MAX_BOARD_DETAIL_VIEW - 1;
        }

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Map<String,  GameBoardBlock> blockMap = new HashMap<>();
        final GameBoardState boardState;
        synchronized (stateStorage) {
            boardState = new GameBoardState(stateStorage.getVersion(), x1, x2, y1, y2);
            for (String blockKey : blockKeys) {
                final GameBoardBlock block = stateStorage.getBlockOrCreate(blockKey, roomSettings().getSymbolDistribution());
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
        if (!roomSettings().getDictionary().isWord(word)) {
            throw new GameNotificationException(invalidWord(word));
        }
        if (roomSettings().hasMaxLetterDistance()) {
            final PlayedTile farTile = letterFarFromOthers(tiles, roomSettings().getMaxLetterDistance());
            if (farTile != null) {
                throw new GameNotificationException(sparseWord(word, roomSettings().getMaxLetterDistance(), farTile));
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
        final GameBoardSettings boardSettings = roomSettings().getBoard().getSettings();
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
        final GameBoardBlock block = stateStorage.getBlockOrCreate(getBlockKeyForTile(x, y), roomSettings().getSymbolDistribution());
        return block.getAbsoluteTile(x, y);
    }

    private Map<String, GameBoardView> cachedViews = new ConcurrentHashMap<>();

    public GameBoardView getBoardView(int x1, int x2, int y1, int y2, int width, int height, GameBoardPalette renderPalette) throws IOException {
        final GameBoard board = roomSettings().getBoard();
        final GameBoardSettings settings = board.getSettings();
        final GameBoardPalette palette = renderPalette == null ? GameBoardPalette.defaultPalette() : renderPalette;

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
            y2 = y1 + MAX_BOARD_VIEW- 1;
        }

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Map<String,  GameBoardBlock> blockMap = new HashMap<>();
        final GameBoardBlock largestBlock = new GameBoardBlock(Integer.MIN_VALUE, Integer.MIN_VALUE);
        final GameBoardBlock smallestBlock = new GameBoardBlock(Integer.MAX_VALUE, Integer.MAX_VALUE);
        synchronized (stateStorage) {
            for (String blockKey : blockKeys) {
                final GameBoardBlock block = stateStorage.getBlockOrCreate(blockKey, roomSettings().getSymbolDistribution());
                blockMap.put(block.getBlockKey(), block);
                if (block.getBlockX() < smallestBlock.getBlockX()) smallestBlock.setBlockX(block.getBlockX());
                if (block.getBlockX() > largestBlock.getBlockX()) largestBlock.setBlockX(block.getBlockX());
                if (block.getBlockY() < smallestBlock.getBlockY()) smallestBlock.setBlockY(block.getBlockY());
                if (block.getBlockY() > largestBlock.getBlockY()) largestBlock.setBlockY(block.getBlockY());
            }
        }

        final long start = now();
        final Collection<Future<?>> futures = new ArrayList<>();
        @Cleanup("shutdownNow") final ExecutorService pool = fixedPool(100);

        final int rawWidth = largestBlock.getX2() - smallestBlock.getX1();
        final int rawHeight = largestBlock.getY2() - smallestBlock.getY1();
        final GameBoardView boardView = new GameBoardView()
                .setRoom(room.getName())
                .setX1(smallestBlock.getX1())
                .setX2(largestBlock.getX2())
                .setY1(smallestBlock.getY1())
                .setY2(largestBlock.getY2())
                .setWidth(rawWidth)
                .setHeight(rawHeight)
                .setPalette(palette);

        final GameBoardView cached = cachedViews.get("" + boardView.hashCode());
        if (cached != null && cached.youngerThan(SECONDS.toMillis(30))) return cached;

        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = bufferedImage.createGraphics();
        final double scaleX = ((double) width) / ((double) rawWidth);
        final double scaleY = ((double) height) / ((double) rawHeight);
        for (GameBoardBlock block : blockMap.values()) {
            //futures.add(pool.submit(() -> {
            // todo: try to load png for block from redis
            // if not found, create it. use graphics2d then save to buffered image
            // no need to adjust indices, just use tiles as is, with 0x0 in [0][0] and so on
            // each block will have 1 tile image

            final double blockX = (block.getBlockX() - smallestBlock.getBlockX()) * scaleX;
            final double blockY = (block.getBlockY() - smallestBlock.getBlockY()) * scaleY;

            final ByteArrayInputStream blockImage = getBlockImage(block, palette);
            BufferedImage bim = ImageIO.read(blockImage);
            AffineTransform xform = new AffineTransform();
            xform.setToTranslation(blockX, blockY);
            xform.setToScale(scaleX, scaleY);
            final ImageObserver imageObserver = new ImageObserver() {
                @Getter private volatile boolean done = false;
                @Override public boolean imageUpdate(Image image, int infoFlags, int x, int y, int width, int height) {
                    log.info("imageUpdate: infoFlags: "+infoFlags);
                    return done;
                }
            };
            if (!g2.drawImage(bim, xform, imageObserver)) {
                // use imageObserver
                log.info("we need to use imageObserver");
            }
            //}));
        }
        //Await.awaitAll(futures, 10*TimeUtil.SECOND);
        final String duration = formatDuration(now() - start);
        log.info("mapping of view took "+ duration);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        boardView.setImage(out.toByteArray());

        // cache image
        cachedViews.put(""+boardView.hashCode(), boardView);

        return boardView;
    }

    private ByteArrayInputStream getBlockImage(GameBoardBlock block, GameBoardPalette palette) {

        final BufferedImage bufferedImage = new BufferedImage(block.getWidth(), block.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final GameTileState[][] tiles = block.getTiles();
        for (int x = 0; x<tiles.length; x++) {
            for (int y = 0; y<tiles[x].length; y++) {
                try {
                    bufferedImage.setRGB(x, y, palette.rgbFor(tiles[x][y]));
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.warn("wtf: "+e);
                }
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", out);
        } catch (IOException e) {
            return die("error writing png of board view: "+e, e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

}
