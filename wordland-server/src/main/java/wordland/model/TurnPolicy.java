package wordland.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.NameAndValue;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.ellipsis;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;

public class TurnPolicy {

    public static final String PARAM_MAX_TURN_DURATION = "max_turn_duration";

    public static final String PARAM_TURN_DURATION = "turn_duration";
    public static final String PARAM_MAX_TURNS = "max_turns";

    @Getter @Setter private String name;
    @Getter @Setter private TurnPolicyType type;

    @Getter @Setter private NameAndValue[] params;
    public String param(String name) { return NameAndValue.find(params, name); }
    public boolean hasParam(String name) { return param(name) != null; }

    public Long longParam(String name) {
        final String val = param(name);
        return empty(val) ? null : Long.parseLong(val);
    }
    public long longParam(String name, long defaultValue) {
        final Long val = longParam(name);
        return val == null ? defaultValue : val;
    }

    public int intParam(String name, int defaultValue) {
        final Long val = longParam(name);
        return val == null ? defaultValue : val.intValue();
    }

    public Long durationParam(String name) {
        final String val = param(name);
        return empty(val) ? null : parseDuration(val);
    }

    @Getter private String message;
    public TurnPolicy setMessage (String m) { message = ellipsis(m, 200); return this; }

}
