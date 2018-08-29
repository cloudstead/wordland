package wordland.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import wordland.model.game.GameBoardPalette;

import java.util.HashSet;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class TextPreviewRequest {

    @Getter @Setter private AttemptedTile[] tiles;
    public boolean hasTiles () { return !empty(tiles); }

    @Getter @Setter private GameBoardPalette palette;
    public boolean hasPalette () { return palette != null; }

    @JsonIgnore public boolean isValid () {
        final Set<AttemptedTile> keys = new HashSet<>();
        for (AttemptedTile tile : tiles) {
            if (keys.contains(tile)) return false;
            keys.add(tile);
        }
        return true;
    }

}
