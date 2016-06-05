package wordland.model.json;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

public class SymbolWeight {

    @Size(max=100, message="err.symbol.tooLong")
    @Getter @Setter private String symbol;
    @Getter @Setter private int weight;

}
