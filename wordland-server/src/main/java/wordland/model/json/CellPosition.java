package wordland.model.json;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @EqualsAndHashCode(of={"x", "y"})
public class CellPosition {

    public int x;
    public int y;

}
