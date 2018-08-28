package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

@Accessors(chain=true)
public class PointSystemSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_PointSystemSettings";

    @Getter @Setter private SymbolScore[] symbolScoring;
    @Getter @Setter private WordScore[] wordScoring;

}
