package wordland.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum SymbolCaseMode {

    upper (String::toUpperCase),
    lower (String::toLowerCase),
    as_is (s -> s);

    final Function<String, String> f;

    @JsonCreator public static SymbolCaseMode fromString (String val) { return valueOf(val.toLowerCase()); }

    public String apply(String symbol) { return f.apply(symbol); }

}
