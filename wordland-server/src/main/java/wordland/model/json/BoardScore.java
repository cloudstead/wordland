package wordland.model.json;

import lombok.Getter;
import lombok.Setter;

public class BoardScore {

    @Getter @Setter private String name;
    @Getter @Setter private String condition;
    @Getter @Setter private String picas;
    @Getter @Setter private boolean absolute;

}
