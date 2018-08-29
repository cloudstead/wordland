package wordland.model.support;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode(of={"x", "y"}) @ToString
public class PlayedTile {

    @Getter @Setter private int x;
    @Getter @Setter private int y;
    @Getter @Setter private String symbol;

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

}
