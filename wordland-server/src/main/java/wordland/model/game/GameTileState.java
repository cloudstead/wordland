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

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

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
        final StringBuilder b = new StringBuilder();
        final AttemptState attemptState = empty(attempt) ? null : new AttemptState(attempt);
        for (int i=0; i<tiles.length; i++) {
            final GameTileState[] row = tiles[i];
            final StringBuilder rowVal = new StringBuilder();
            for (int j=0; j<row.length; j++) {
                final GameTileState tile = row[j];
                if (attemptState != null) {
                    attemptState.tryMatch(tile, i, j, tiles);
                }
                if (rowVal.length() > 0) {
                    rowVal.append(TXT_SPACER);
                } else {
                    if (palette != null) {
                        rowVal.append(i).append(i < 10 ? TXT_SPACER : TXT_SHORT_SPACER);
                    }
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
            final StringBuilder header = new StringBuilder();
            for (int i=0; i<tiles.length; i++) {
                if (header.length() > 0) {
                    if (i <= 10) header.append(TXT_SPACER);
                    else header.append(TXT_SHORT_SPACER);
                }
                header.append(i);
            }
            header.append("\n");
            header.insert(0, "    ");
            header.insert(0, setFgColor(new Color(palette.getBlankColorRgb())));
            b.insert(0, header);
            b.append("\\033[0m");
        }

        final TextGridResponse text = new TextGridResponse().setGrid(b.toString());
        if (attemptState != null) {
            text.setPlayedTiles(attemptState.getPlayedTiles());
            text.setSuccess(attemptState.successful());
        }
        return text;
    }

    protected static String setFgColor(Color color) {
        StringBuilder b = new StringBuilder();
        b.append("\\033[38;2;")
                .append(color.getRed()).append(";")
                .append(color.getGreen()).append(";")
                .append(color.getBlue()).append("m");
        return b.toString();
    }

    private static class AttemptState {
        private final AttemptedTile[] attempt;
        private final int[] counters;
        @Getter private final PlayedTile[] playedTiles;

        public boolean successful () {
            for (PlayedTile p : playedTiles) if (p == null) return false;
            return true;
        }

        public AttemptState (AttemptedTile[] attempt) {
            this.attempt = attempt;
            this.counters = new int[attempt.length];
            this.playedTiles = new PlayedTile[attempt.length];
        }

        public void tryMatch(GameTileState tile, int x, int y, GameTileState[][] tiles) {
            for (int i=0; i<attempt.length; i++) {
                final AttemptedTile a = attempt[i];

                boolean ok = true;
                for (PlayedTile p : playedTiles) {
                    if (p == null) continue;
                    if (p.getX() == x && p.getY() == y) {
                        // already played
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;

                if (a.getSymbol().equalsIgnoreCase(tile.getSymbol())) {
                    if (a.getIndex() == counters[i]+1) {
                        tile.setOwner(a.getOwner());
                        if (canPlay(tile, x, y, tiles, a.getOwner())) {
                            tile.addFeature(F_PREVIEW_PLAY, Boolean.TRUE.toString());
                        } else {
                            tile.addFeature(F_PREVIEW_PLAY_BLOCKED, Boolean.TRUE.toString());
                        }
                        playedTiles[i] = new PlayedTile(x, y, a.getSymbol());
                    } else {
                        counters[i]++;
                    }
                }
            }
        }

        private boolean canPlay(GameTileState tile, int x, int y, GameTileState[][] tiles, String owner) {
            // todo: check can we play here? it may be another player's protected tile
            return true;
        }
    }
}
