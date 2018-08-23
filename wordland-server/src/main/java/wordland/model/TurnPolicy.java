package wordland.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.NameAndValue;

import static org.cobbzilla.util.string.StringUtil.ellipsis;

public class TurnPolicy {

    @Getter @Setter private String name;
    @Getter @Setter private TurnPolicyType type;
    @Getter @Setter private NameAndValue[] params;
    @Getter private String message;
    public TurnPolicy setMessage (String m) { message = ellipsis(m, 200); return this; }

}
