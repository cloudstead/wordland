package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import javax.validation.constraints.Size;

public class SymbolScore {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_SymbolScoring";

    @Size(max=100, message="err.symbol.tooLong")
    @Getter @Setter private String symbol;
    @Getter @Setter private int points;

}
