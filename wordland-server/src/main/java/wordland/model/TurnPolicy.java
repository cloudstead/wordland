package wordland.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.NameAndValue;

import static org.cobbzilla.util.string.StringUtil.ellipsis;

public class TurnPolicy {

    public static final String PARAM_MAX_TURN_DURATION = "max_turn_duration";
    @Getter @Setter private String name;
    @Getter @Setter private TurnPolicyType type;

    @Getter @Setter private NameAndValue[] params;
    public String param(String name) { return NameAndValue.find(params, name); }
    public long longParam(String name) { return Long.parseLong(param(name)); }

    @Getter private String message;
    public TurnPolicy setMessage (String m) { message = ellipsis(m, 200); return this; }

}
