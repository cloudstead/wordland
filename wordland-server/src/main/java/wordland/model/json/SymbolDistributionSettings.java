package wordland.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.daemon.ZillaRuntime.pickRandom;

@Accessors(chain=true)
public class SymbolDistributionSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_SymbolDistributionSettings";

    @Valid @Getter @Setter private SymbolWeight[] weights;

    @JsonIgnore @Getter(lazy=true) private final Iterator<String> picker = initPicker();
    private Iterator<String> initPicker () {
        final List<String> expanded = new ArrayList<>();
        for (SymbolWeight weight : weights) {
            for (int i=0; i<weight.getWeight(); i++) expanded.add(weight.getSymbol());
        }
        return new Iterator<String>() {
            @Override public boolean hasNext() { return true; }
            @Override public void remove() { notSupported(); }
            @Override public String next() { return pickRandom(expanded); }
        };
    }

}
