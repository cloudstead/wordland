package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileState {

    public static final GameTileState[][] EMPTY_STATE = new GameTileState[0][0];

    @Getter @Setter private String symbol;
    @Getter @Setter private String owner;
    public boolean hasOwner () { return owner != null; }
    public boolean unclaimed () { return !hasOwner(); }

    public static String grid (GameTileState[][] tiles) {
        final StringBuilder b = new StringBuilder();
        for (GameTileState[] row : tiles) {
            final StringBuilder rowVal = new StringBuilder();
            for (GameTileState tile : row) {
                if (rowVal.length() > 0) {
                    rowVal.append(" | ");
                }
                rowVal.append(tile.getSymbol());
            }
            if (b.length() > 0) b.append("\n");
            b.append(rowVal);
        }
        return b.toString();
    }
}
