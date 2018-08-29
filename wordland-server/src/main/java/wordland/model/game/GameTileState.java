package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;
import wordland.model.support.AttemptedTile;
import wordland.model.support.PlayedTile;
import wordland.model.support.TextGridResponse;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.ShaUtil.sha256;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileState {

    public static final String F_PREVIEW_PLAY = "preview_play";
    public static final String F_PREVIEW_PLAY_BLOCKED = "preview_play_blocked";

    public static final String TXT_SPACER = "   ";
    public static final String TXT_SHORT_SPACER = "  ";

    @Getter @Setter private String symbol;
    @Getter @Setter private String owner;
    @Getter @Setter private NameAndValue[] features;

    public String feature (String name) { return NameAndValue.find(features, name); }
    public boolean hasFeature (String name) { return !empty(feature(name)); }
    public boolean isTrue (String name) { return hasFeature(name) && Boolean.valueOf(feature(name)); }
    public void addFeature(String name, String value) {
        this.features = ArrayUtil.append(features, new NameAndValue(name, value));
    }

    public GameTileState(GameTileState other) {
        setSymbol(other.getSymbol());
        setOwner(other.getOwner());
    }

    public boolean hasOwner () { return owner != null; }
    public boolean unclaimed () { return !hasOwner(); }

    public static String grid (GameTileState[][] tiles) {
        return grid(tiles, null, null).getGrid();
    }

    public static String grid (GameTileState[][] tiles, GameBoardPalette palette) {
     return grid(tiles, null, null).getGrid();
    }

    public static TextGridResponse grid (GameTileState[][] tiles, GameBoardPalette palette, AttemptedTile[] attempt) {

        final TextGridResponse text = new TextGridResponse();
        if (!empty(attempt)) {
            final AttemptState attemptState = new AttemptState(attempt, tiles);
            text.setPlayedTiles(attemptState.getPlayedTilesArray());
            text.setSuccess(attemptState.successful());
        }

        final StringBuilder b = new StringBuilder();
        for (int i=0; i<tiles.length; i++) {
            final GameTileState[] row = tiles[i];
            final StringBuilder rowVal = new StringBuilder();
            for (int j=0; j<row.length; j++) {
                final GameTileState tile = row[j];
                if (rowVal.length() > 0) {
                    rowVal.append(TXT_SPACER);
                }
                if (palette != null) {
                    final Color color;
                    color = new Color(palette.rgbFor(tile));
                    rowVal.append(setFgColor(color));
                }
                rowVal.append(tile.getSymbol().toUpperCase());
            }
            if (b.length() > 0) b.append("\n");
            b.append(rowVal);
        }

        if (palette != null) {
            b.append("\\033[0m");
        }

        return text.setGrid(b.toString());
    }

    protected static String setFgColor(Color color) {
        final StringBuilder b = new StringBuilder();
//        b.append("\\033[38;2;")
        final byte[] bytes = sha256("" + color.getRGB());
        final int validRange = 231 - 17;
        b.append("\\033[38;5;")
                .append((Math.abs((int) bytes[0]) % (validRange))+17)
                .append("m");
//                .append(color.getRed()).append(";")
//                .append(color.getGreen()).append(";")
//                .append(color.getBlue()).append("m");
        return b.toString();
    }

    private static class AttemptState {
        private final AttemptedTile[] attempt;

        @Getter private final Set<PlayedTile> playedTiles = new HashSet<>();

        public PlayedTile[] getPlayedTilesArray () { return playedTiles.toArray(new PlayedTile[playedTiles.size()]); }

        public boolean successful () { return playedTiles.size() == attempt.length; }

        public AttemptState (AttemptedTile[] attempt, GameTileState[][] tiles) {
            this.attempt = attempt;
            for (int i=0; i<attempt.length; i++) {
                final AttemptedTile a = attempt[i];
                boolean played = tryPlay(a, tiles, true);
                if (!played) {
                    // try to find a match that doesn't care about which index it uses
                    played = tryPlay(a, tiles, false);
                }
            }
        }

        protected boolean tryPlay(AttemptedTile a, GameTileState[][] tiles, boolean requireIndexMatch) {
            letterCounters.clear();
            for (int x=0; x<tiles.length; x++) {
                for (int y=0; y<tiles[x].length; y++) {
                    final String symbol = tiles[x][y].getSymbol();
                    final int foundCount = getCountAndIncrement(symbol);
                    if (!a.getSymbol().equalsIgnoreCase(symbol)) continue;

                    final PlayedTile playedTile = new PlayedTile(x, y, symbol);
                    if (playedTiles.contains(playedTile)) {
                        // already played, try another
                        continue;
                    }

                    // is it the index they want?
                    if (requireIndexMatch && a.getIndex() != foundCount) continue;

                    // OK, pick the letter
                    tiles[x][y].setOwner(a.getOwner());
                    playedTiles.add(playedTile);
                    return true;
                }
            }
            return false;
        }

        private Map<String, Integer> letterCounters = new HashMap<>();
        private int getCountAndIncrement(String symbol) {
            final int val = letterCounters.computeIfAbsent(symbol.toLowerCase(), k -> 0);
            letterCounters.put(symbol.toLowerCase(), val+1);
            return val;
        }

        private boolean canPlay(GameTileState tile, int x, int y, GameTileState[][] tiles, String owner) {
            // todo: check can we play here? it may be another player's protected tile
            return true;
        }
    }
}
