package wordland.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.string.StringUtil;

@Accessors(chain=true)
public class BonusPolicy {

    @Getter @Setter private String name;
    @Getter @Setter private String bonus;
    @Getter @Setter private BonusPolicyType type;
    @Getter @Setter private NameAndValue[] params;
    @Getter private String message;
    public BonusPolicy setMessage (String m) {
        message = StringUtil.ellipsis(m, 200);
        return this;
    }

}
