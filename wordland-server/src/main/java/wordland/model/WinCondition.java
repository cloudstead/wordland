package wordland.model;

import lombok.Getter;
import lombok.Setter;

public class WinCondition {

    @Getter @Setter private String name;
    @Getter @Setter private String defaultMessage;
    @Getter @Setter private String endJs;
    @Getter @Setter private String winnersJs;

}
