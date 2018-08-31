package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.graphics.ColorUtil;
import wordland.model.support.AttemptedTile;
import wordland.model.support.PlayedTile;
import wordland.model.support.TextGridResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileState {

    public static final String F_PREVIEW_PLAY = "preview_play";
    public static final String F_PREVIEW_PLAY_BLOCKED = "preview_play_blocked";

    public static final int F_PREVIEW_PLAY_BG = 240;
    public static final int F_PREVIEW_PLAY_BLOCKED_BG = 249;

    public static final String TXT_SPACER = "   ";

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
     return grid(tiles, palette, null).getGrid();
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
                if (rowVal.length() > 0) rowVal.append(TXT_SPACER);               // add spacer if after first element
                if (palette != null) rowVal.append(setAnsiColors(tile, palette)); // set colors if we have a palette
                rowVal.append(tile.getSymbol().toUpperCase());                    // write tile symbol
                if (palette != null) rowVal.append(ColorUtil.ANSI_RESET);                   // reset colors
            }
            if (b.length() > 0) b.append("\n");
            b.append(rowVal);
        }

        return text.setGrid(b.toString());
    }

    protected static String setAnsiColors(GameTileState tile, GameBoardPalette palette) {
        final StringBuilder b = new StringBuilder();
        int fg = palette.colorFor(tile);
        Integer bg = null;
        if (tile.hasFeature(F_PREVIEW_PLAY)) {
            bg = F_PREVIEW_PLAY_BG;
        } else if (tile.hasFeature(F_PREVIEW_PLAY_BLOCKED)) {
            bg = F_PREVIEW_PLAY_BLOCKED_BG;
        }
        b.append("\\033[38;5;")
                .append(fg)
                .append(bg == null ? "" : ";48;5;"+bg)
                .append("m");
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
                    if (canPlay(tiles[x][y], x, y, tiles, a.getOwner())) {
                        tiles[x][y].addFeature(F_PREVIEW_PLAY, Boolean.TRUE.toString());
                    } else {
                        tiles[x][y].addFeature(F_PREVIEW_PLAY_BLOCKED, Boolean.TRUE.toString());
                    }
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
