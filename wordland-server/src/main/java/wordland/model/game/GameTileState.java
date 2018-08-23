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

}
