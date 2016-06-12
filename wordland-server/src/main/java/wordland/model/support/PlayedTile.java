package wordland.model.support;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @ToString
public class PlayedTile {

    @Getter @Setter private int x;
    @Getter @Setter private int y;
    @Getter @Setter private String symbol;

}
