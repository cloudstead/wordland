package wordland.model.support;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode(of={"symbol", "index"})
@ToString(of={"symbol", "index"})
public class AttemptedTile {

    @Getter @Setter private String owner;
    @Getter @Setter private String symbol;
    @Getter @Setter private int index;

}
