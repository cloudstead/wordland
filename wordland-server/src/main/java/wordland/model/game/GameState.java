package wordland.model.game;

import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.AwaitResult;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.time.TimeUtil;
import wordland.model.*;
import wordland.model.game.score.PlayScore;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.Await.awaitAll;
import static org.cobbzilla.util.daemon.DaemonThreadFactory.fixedPool;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.string.ValidationRegexes.UUID_PATTERN;
import static org.cobbzilla.util.time.TimeUtil.DATE_FORMAT_YYYY_MM_DD_HH_mm_ss;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static wordland.ApiConstants.MAX_BOARD_DETAIL_VIEW;
import static wordland.ApiConstants.MAX_BOARD_VIEW;
import static wordland.model.GameBoardBlock.SORT_POSITION;
import static wordland.model.GameBoardBlock.getBlockKeyForTile;
import static wordland.model.game.GameStateChange.playerJoined;
import static wordland.model.game.GameStateChangeType.word_played;
import static wordland.model.support.GameNotification.invalidWord;
import static wordland.model.support.GameNotification.sparseWord;
import static wordland.model.support.PlayedTile.letterFarFromOthers;

@Slf4j
public class GameState {

    public static final int MAX_BOARD_IMAGE_WIDTH = 1000;
    public static final int MAX_BOARD_IMAGE_HEIGHT = 1000;
    public static final int TILE_PIXEL_SIZE = 10;
    public static final double TILE_PIXEL_SIZE_DOUBLE = (double) TILE_PIXEL_SIZE;
    public static final long BOARD_RENDER_TIMEOUT = 20 * TimeUtil.SECOND;

    public static final String CTX_PLAYER = "player";
    public static final String CTX_PLAYERS = "players";
    public static final String CTX_WORD = "word";
    public static final String CTX_TILES = "tiles";
    public static final String CTX_SCOREBOARD = "scoreboard";

    private final GameRoom room;
    @Getter private final GameStateStorageService stateStorage;

    public GameState(GameRoom room, GameStateStorageService storage) {
        this.room = room;
        this.stateStorage = storage;
    }

    private GameRoomSettings roomSettings() { return room.getSettings(); }

    public GamePlayer getPlayer(String id) { return stateStorage.getPlayer(id); }
    public GamePlayerExitStatus getPlayerExitStatus(String uuid) { return stateStorage.getPlayerExitStatus(uuid); }

    public Collection<GamePlayer> getPlayers() { return stateStorage.getPlayers(); }
    public Map<String, GamePlayer> getCurrentAndFormerPlayers() { return stateStorage.getCurrentAndFormerPlayers(); }
    public Map<String, Integer> getScoreboard() { return stateStorage.getScoreboard(); }
    public RoomState getRoomState() { return stateStorage.getRoomState(); }

    public Collection<String> getWinners() { return stateStorage.getWinners(); }
    public boolean hasWinners () { return !empty(getWinners()); }

    public GameStateChange addPlayer(GamePlayer player) {
        synchronized (stateStorage) {
            final RoomState roomState = stateStorage.getRoomState();

            if (getPlayer(player.getId()) != null) {
                // player already in room, start new room session
                return playerJoined(stateStorage.getVersion(), player);
            }

            if (roomState == RoomState.ended) throw new GameEndedException(room);

            int playerCount = stateStorage.getPlayerCount();

            // todo: if maxPlayers reached, see if any can be evicted? maybe not, let GameDaemon handle that...
            if (roomSettings().hasMaxPlayers() && playerCount >= roomSettings().getMaxPlayers()) throw new RoomFullException(room);

            if (roomState == RoomState.waiting
                    && (!roomSettings().hasMinPlayersToStart() || playerCount+1 >= roomSettings().getMinPlayersToStart())) {
                return stateStorage.addPlayerStartGame(player);

            } else {
                return stateStorage.addPlayer(player);
            }
        }
    }

    public GameStateChange removePlayer(String id) {
        return stateStorage.removePlayer(id);
    }

    public GameBoardState getBoard(int x1, int x2, int y1, int y2) {

        final GameBoard board = roomSettings().getBoard();
        final GameBoardSettings settings = board.getSettings();

        if (settings.hasWidth()) {
            if (x1 < 0) x1 = 0;
            if (x2 > settings.getWidth()) x2 = settings.getWidth()-1;
        }
        if (x2 - x1 >= MAX_BOARD_DETAIL_VIEW) {
            x2 = x1 + MAX_BOARD_DETAIL_VIEW - 1;
        }
        if (settings.hasLength()) {
            if (y1 < 0) y1 = 0;
            if (y2 > settings.getLength()) y2 = settings.getLength()-1;
        }
        if (y2 - y1 >= MAX_BOARD_DETAIL_VIEW) {
            y2 = y1 + MAX_BOARD_DETAIL_VIEW - 1;
        }

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Map<String,  GameBoardBlock> blockMap = new HashMap<>();
        final GameBoardState boardState;
        synchronized (stateStorage) {
            boardState = new GameBoardState(stateStorage.getVersion(), x1, x2, y1, y2, stateStorage.getRoomState());
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
        awaitAll(futures, TILE_PIXEL_SIZE *TimeUtil.SECOND);
        final String duration = formatDuration(now() - start);
        log.info("mapping of blocks took "+ duration);

        return boardState.setTiles(tiles);
    }

    public static final JsEngine JS = new StandardJsEngine();

    public GameStateChange playWord(GamePlayer player, String word, PlayedTile[] tiles) {

        if (stateStorage.isGameOver()) throw invalidEx("err.game.gameOver");

        if (word.length() < 2) {
            throw new GameNotificationException(invalidWord(word));
        }

        final GameRoomSettings rs = roomSettings();
        if (!rs.getDictionary().isWord(word)) {
            throw new GameNotificationException(invalidWord(word));
        }

        if (rs.hasMaxLetterDistance()) {
            final PlayedTile farTile = letterFarFromOthers(tiles, rs.getMaxLetterDistance());
            if (farTile != null) {
                throw new GameNotificationException(sparseWord(word, rs.getMaxLetterDistance(), farTile));
            }
        }

        // do these tiles correspond to this word?
        if (tiles.length != word.length()) {
            throw new GameNotificationException(invalidWord(word));
        }
        final StringBuffer buf = new StringBuffer(word);
        while (buf.length() > 0) {
            int index = PlayedTile.indexOf(tiles, buf.charAt(0));
            if (index == -1) {
                throw new GameNotificationException(invalidWord(word));
            }
            buf.delete(0, 1);
        }

        final PointSystem pointSystem = rs.getPointSystem();
        final PlayScore playScore = new PlayScore().setPlayer(player);
        final Map<String, GameBoardBlock> alteredBlocks = new HashMap<>();
        final List<PlayedTile> claimableTiles = new ArrayList<>();
        synchronized (stateStorage) {

            if (stateStorage.isGameOver()) throw invalidEx("err.game.gameOver");

            // is it our turn?
            if (rs.hasRoundRobinPolicy()) {
                final String nextPlayerId = stateStorage.getCurrentPlayerId();
                if (nextPlayerId == null || !nextPlayerId.equals(player.getId())) throw new NotYourTurnException();
            }

            // determine applicable board blocks, fromString change set
            for (int i=0; i<tiles.length; i++) {
                final PlayedTile tile = tiles[i];
                final String blockKey = getBlockKeyForTile(tile.getX(), tile.getY());
                final GameBoardBlock block = alteredBlocks.computeIfAbsent(blockKey, (k) -> stateStorage.getBlock(blockKey));
                if (block == null) return die("playWord: uninitialized block");

                final GameTileState boardTile = block.getTiles()[tile.getX() - block.getX1()][tile.getY() - block.getY1()];
                if (!boardTile.getSymbol().equals(tile.getSymbol())) {
                    return die("playWord: invalid play");
                }
                if (isClaimableByPlayer(player, boardTile, tile.getX(), tile.getY())) {
                    boardTile.setOwner(player.getId());
                    alteredBlocks.put(block.getBlockKey(), block);
                    playScore.addScore(pointSystem.scoreLetter(tile));
                    claimableTiles.add(tile);
                }
            }

            playScore.addScore(pointSystem.scoreWord(word));

            final Map<String, Object> jsContext = pointSystem.hasBoardScoring() || rs.hasWinConditions()
                    ? getJsContext(player, getPlayers(), word, tiles, rs, claimableTiles)
                    : null;
            playScore.addScores(pointSystem.scoreBoard(jsContext));

            Collection<String> winners = null;
            if (rs.hasWinConditions()) {
                for (WinCondition w : rs.getWinConditions()) {
                    if (JS.evaluateBoolean(w.getEndJs(), jsContext, false)) {
                        final Object rawWinners = JS.evaluate(w.getWinnersJs(), jsContext);
                        if (rawWinners instanceof Collection) {
                            winners = new ArrayList<>((Collection<String>) rawWinners);
                        } else if (rawWinners.getClass().isArray()) {
                            winners = new ArrayList<>(Arrays.asList((String[]) rawWinners));
                        } else {
                            winners = StringUtil.split(rawWinners.toString(), ", \n\t");
                        }
                        for (String winner : winners) {
                            if (!UUID_PATTERN.matcher(winner).matches()) {
                                return die("playWord: invalid winner: "+winner);
                            }
                        }
                    }
                }
            }

            return stateStorage.playWord(player, alteredBlocks.values(), word, tiles, playScore, winners);
        }
    }

    protected Map<String, Object> getJsContext(GamePlayer player,
                                               Collection<GamePlayer> players,
                                               String word,
                                               PlayedTile[] tiles,
                                               GameRoomSettings rs,
                                               List<PlayedTile> claimableTiles) {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put(CTX_PLAYER, player);
        ctx.put(CTX_PLAYERS, players.stream().collect(Collectors.toMap(GamePlayer::getId, Function.identity())));
        ctx.put(CTX_WORD, word);
        ctx.put(CTX_TILES, tiles);
        ctx.put(CTX_SCOREBOARD, getScoreboard());
        if (!rs.getBoard().infinite()) {
            final GameBoardSettings bs = rs.getBoard().getSettings();
            final GameBoardState board = getBoard(0, bs.getWidth()-1, 0, bs.getLength()-1);
            if (board.getX2() - board.getX1() + 1 != bs.getWidth() || board.getY2() - board.getY1() + 1 != bs.getLength()) {
                log.warn("getJsContext: board is too big, cannot be used in JS expressions");
            } else {
                board.setOwner(player.getId(), claimableTiles);
                ctx.put("board", board);
            }
        }
        return ctx;
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

    private String timestamp() { return TimeUtil.format(now(), TimeUtil.DATE_FORMAT_YYYYMMDDHHMMSS); }

    private Map<String, GameBoardView> cachedViews = new ConcurrentHashMap<>();

    public GameBoardView getBoardView(int x1, int x2, int y1, int y2,
                                      int imageWidth, int imageHeight,
                                      GameBoardPalette palette,
                                      boolean noCache) throws IOException {

        final GameBoard board = roomSettings().getBoard();
        final GameBoardSettings settings = board.getSettings();

        // ensure the bounds we draw are not too large, and certainly not larger than the board view
        if (settings.hasWidth()) {
            // if settings has a width, X must always be >= 0
            if (x1 < 0) x1 = 0;

            // cannot ask for something bigger than MAX_BOARD_VIEW
            if (x2 - x1 >= MAX_BOARD_VIEW) x2 = x1 + MAX_BOARD_VIEW - 1;

            // cannot ask for something bigger than the board itself
            if (x2 > settings.getWidth()) x2 = settings.getWidth()-1;

        } else if (x2 - x1 >= MAX_BOARD_VIEW) {
            // infinite board width, still cannot view more than MAX_BOARD_VIEW
            x2 = x1 + MAX_BOARD_VIEW - 1;
        }
        if (settings.hasLength()) {
            // if settings has a length, Y must always be >= 0
            if (y1 < 0) y1 = 0;

            // cannot ask for something bigger than MAX_BOARD_VIEW
            if (y2 - y1 >= MAX_BOARD_VIEW) y2 = y1 + MAX_BOARD_VIEW - 1;

            // cannot ask for something bigger than the board itself
            if (y2 > settings.getLength()) y2 = settings.getLength()-1;

        } else if (y2 - y1 >= MAX_BOARD_VIEW) {
            // infinite board length, still cannot view more than MAX_BOARD_VIEW
            y2 = y1 + MAX_BOARD_VIEW - 1;
        }

        // ensure output image is within bounds
        if (imageWidth > MAX_BOARD_IMAGE_WIDTH) imageWidth = MAX_BOARD_IMAGE_WIDTH;
        if (imageHeight > MAX_BOARD_IMAGE_HEIGHT) imageHeight = MAX_BOARD_IMAGE_HEIGHT;

        // determine applicable board blocks
        final Collection<String> blockKeys = GameBoardBlock.getBlockKeys(x1, x2, y1, y2);

        // fetch blocks, initialize as needed
        final Set<GameBoardBlock> blocks = new TreeSet<>(SORT_POSITION);
        final GameBoardBlock largestBlock = new GameBoardBlock(Integer.MIN_VALUE, Integer.MIN_VALUE);
        final GameBoardBlock smallestBlock = new GameBoardBlock(Integer.MAX_VALUE, Integer.MAX_VALUE);
        synchronized (stateStorage) {
            for (String blockKey : blockKeys) {
                final GameBoardBlock block = stateStorage.getBlockOrCreate(blockKey, roomSettings().getSymbolDistribution());
                blocks.add(block);
                if (block.getBlockX() < smallestBlock.getBlockX()) smallestBlock.setBlockX(block.getBlockX());
                if (block.getBlockX() > largestBlock.getBlockX()) largestBlock.setBlockX(block.getBlockX());
                if (block.getBlockY() < smallestBlock.getBlockY()) smallestBlock.setBlockY(block.getBlockY());
                if (block.getBlockY() > largestBlock.getBlockY()) largestBlock.setBlockY(block.getBlockY());
            }
        }

        final long start = now();

        final int xMax = xMax(x2, largestBlock);
        final int rawWidth = xMax - smallestBlock.getX1() + 1;
        final int yMax = yMax(y2, largestBlock);
        final int rawHeight = yMax - smallestBlock.getY1() + 1;
        final GameBoardView boardView = new GameBoardView()
                .setRoom(room.getName())
                .setX1(x1).setX2(x2)
                .setY1(y1).setY2(y2)
                .setTilesWidth(rawWidth)
                .setTilesHeight(rawHeight)
                .setImageWidth(imageWidth)
                .setImageHeight(imageHeight)
                .setPalette(palette);

        noCache = true;
        final GameBoardView cached = noCache ? null : cachedViews.get("" + boardView.hashCode());
        if (cached != null && cached.youngerThan(SECONDS.toMillis(30))) return cached.setRoomState(stateStorage.getRoomState());

        final BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = bufferedImage.createGraphics();
        final double scaleX = ((double) imageWidth) / ((double) rawWidth);
        final double scaleY = ((double) imageHeight) / ((double) rawHeight);
        final AffineTransform xform = new AffineTransform();
        xform.setToScale(scaleX/TILE_PIXEL_SIZE_DOUBLE, scaleY/TILE_PIXEL_SIZE_DOUBLE);

        final int imHeight = imageHeight;
        final int imWidth = imageWidth;

        palette.init();

        final Collection<Future<?>> futures = new ArrayList<>();
        @Cleanup("shutdownNow") final ExecutorService pool = fixedPool(100);
        for (GameBoardBlock block : blocks) {
            futures.add(pool.submit(() -> {
                // X/Y position for this block image on the final image
                final double blockX = ((block.getBlockX() - smallestBlock.getBlockX()) / ((double)xMax+1.0d)) * imWidth;
                final double blockY = ((block.getBlockY() - smallestBlock.getBlockY()) / ((double)yMax+1.0d)) * imHeight;
                log.info(">>>> blockX="+((int)blockX)+", blockY="+((int)blockY));

                final ByteArrayInputStream blockImage = getBlockImage(block, palette, rawWidth, rawHeight);
                final BufferedImage bim;
                try {
                    bim = ImageIO.read(blockImage);
                } catch (IOException e) {
                    die("getBoardView: error reading block ("+block+"): "+e, e);
                    return;
                }
                synchronized (g2) {
                    g2.drawImage(bim, new AffineTransformOp(xform, AffineTransformOp.TYPE_BICUBIC), (int) blockX, (int) blockY);
                }
//              final FileOutputStream fileOut = new FileOutputStream("/tmp/views/partial_"+block.getBlockX()+"_"+block.getBlockY()+".png");
//              ImageIO.write(bufferedImage, "png", fileOut);
//              log.info("done drawing: "+fileOut);
            }));

        }
        final AwaitResult<Object> result = awaitAll(futures, BOARD_RENDER_TIMEOUT);
        final String duration = formatDuration(now() - start);
        log.info("mapping of view took "+ duration);
        if (!result.allSucceeded()) {
            return die("getBoardView: timeout creating view");
        }

        // write timestamp
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, 10));
        g2.setColor(Color.BLACK);
        g2.drawString(TimeUtil.format(now(), DATE_FORMAT_YYYY_MM_DD_HH_mm_ss), imageWidth-110, imageHeight-8);

        // write to file
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        boardView.setImage(out.toByteArray());

        //final FileOutputStream fileOut = new FileOutputStream("/tmp/views/complete_"+timestamp()+".png");
        //ImageIO.write(bufferedImage, "png", fileOut);

        // cache image
        cachedViews.put(""+boardView.hashCode(), boardView);

        return boardView.setRoomState(stateStorage.getRoomState());
    }

    protected int yMax(int y2, GameBoardBlock largestBlock) {
        return Math.min(y2, largestBlock.getY2());
    }

    protected int xMax(int x2, GameBoardBlock largestBlock) {
        return Math.min(x2, largestBlock.getX2());
    }

    private ByteArrayInputStream getBlockImage(GameBoardBlock block, GameBoardPalette palette, int maxX, int maxY) {

        final BufferedImage bufferedImage = new BufferedImage(
                TILE_PIXEL_SIZE*Math.min(block.getWidth(), maxX),
                TILE_PIXEL_SIZE*Math.min(block.getHeight(), maxY),
                BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = bufferedImage.createGraphics();
        final GameTileStateExtended[][] tiles = block.getTilesExtended();
        for (int x=0; x<tiles.length && x<=maxX; x++) {
            for (int y=0; y<tiles[x].length && y <=maxY; y++) {
                g2.setColor(new Color(palette.colorFor(tiles[x][y])));
                try {
                    g2.fillRect(y*TILE_PIXEL_SIZE, x*TILE_PIXEL_SIZE, TILE_PIXEL_SIZE, TILE_PIXEL_SIZE);
                    // bufferedImage.setRGB(x, y, palette.rgbColorFor(tiles[x][y]));
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

        // save image for now
//        final String filename = "/tmp/views/block_" + block.getBlockKey().replace("/", "_") + "-" + timestamp() + ".png";
//        try (OutputStream fileOut = new FileOutputStream(filename)) {
//            IOUtils.write(out.toByteArray(), fileOut);
//        } catch (Exception e) {
//            return die("error saving to disk: "+e, e);
//        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public List<GameRuntimeEvent> getPlays() { return stateStorage.getEvents(word_played); }

}
