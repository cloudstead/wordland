package wordland.model.support;

import lombok.*;
import lombok.experimental.Accessors;
import wordland.model.TileXYS;
import wordland.model.game.GameTileState;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode(of={"x", "y"}) @ToString
public class PlayedTile implements TileXYS {

    @Getter @Setter private int x;
    @Getter @Setter private int y;
    @Getter @Setter private String symbol;

    public PlayedTile(TileXYS tile) {
        setX(tile.getX());
        setY(tile.getY());
        setSymbol(tile.getSymbol());
    }

    public static PlayedTile letterFarFromOthers(PlayedTile[] tiles, int allowedMax) {
        for (PlayedTile t1 : tiles) {
            boolean ok = false;
            for (PlayedTile t2 : tiles) {
                if (t1 == t2) continue;
                int dx = Math.abs(t2.getX() - t1.getX());
                int dy = Math.abs(t2.getY() - t1.getY());
                if (dx <= allowedMax && dy <= allowedMax) {
                    ok = true;
                    break;
                }
            }
            if (!ok) return t1;
        }
        return null;
    }

    public static void claimTiles(String playerId, GameTileState[][] boardTiles, PlayedTile[] playedTiles) {
        for (PlayedTile t : playedTiles) boardTiles[t.getX()][t.getY()].setOwner(playerId);
    }

    public static int indexOf(PlayedTile[] tiles, char c) {
        for (int i=0; i<tiles.length; i++) {
            if (tiles[i].getSymbol().equalsIgnoreCase(""+c)) return i;
        }
        return -1;
    }
}
