package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import javax.validation.Valid;

@Accessors(chain=true)
public class SymbolDistributionSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_SymbolDistributionSettings";

    @Valid @Getter @Setter private SymbolWeight[] weights;

}
