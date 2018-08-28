package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileState {

    public static final GameTileState[][] EMPTY_STATE = new GameTileState[0][0];
    public static final String TXT_SPACER = " | ";
    public static final String TXT_SHORT_SPACER = "| ";

    @Getter @Setter private String symbol;
    @Getter @Setter private String owner;

    public GameTileState(GameTileState other) {
        setSymbol(other.getSymbol());
        setOwner(other.getOwner());
    }

    public boolean hasOwner () { return owner != null; }
    public boolean unclaimed () { return !hasOwner(); }

    public static String grid (GameTileState[][] tiles) {
        return grid(tiles, null);
    }

    public static String grid (GameTileState[][] tiles, GameBoardPalette palette) {
        final StringBuilder b = new StringBuilder();
        for (int i=0; i<tiles.length; i++) {
            final GameTileState[] row = tiles[i];
            final StringBuilder rowVal = new StringBuilder();
            for (int j=0; j<row.length; j++) {
                final GameTileState tile = row[j];
                if (rowVal.length() > 0) {
                    rowVal.append(TXT_SPACER);
                } else {
                    if (palette != null) {
                        rowVal.append(i).append(i <= 10 ? TXT_SPACER : TXT_SHORT_SPACER);
                    }
                }
                rowVal.append(tile.getSymbol());
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
            b.insert(0, header);
        }

        return b.toString();
    }
}
