package wordland.model.game;

import lombok.Getter;
import lombok.Setter;

public class GameTileState {

    @Getter @Setter private String symbol;
    @Getter @Setter private String owner;
    public boolean hasOwner () { return owner != null; }

}
