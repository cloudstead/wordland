package wordland.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class PlayedTile {

    @Getter @Setter private int x;
    @Getter @Setter private int y;
    @Getter @Setter private String symbol;

}
