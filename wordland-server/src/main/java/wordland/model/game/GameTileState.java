package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;
import wordland.model.TileSymbol;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class GameTileState implements TileSymbol {

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
    public boolean hasOwner(String id) { return hasOwner() && getOwner().equals(id); }

    public boolean same(String word, int i) { return getSymbol().charAt(0) == word.charAt(i); }
    public boolean same(String word) { return same(word, 0); }

}
