package wordland.image;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true) @EqualsAndHashCode
public class TileImageSettings {

    @Getter @Setter private String symbol;
    @Getter @Setter private int size = 64;
    @Getter @Setter private int border = 1;
    @Getter @Setter private int fgColor = 0x000000;
    @Getter @Setter private int bgColor = 0xffffff;
    @Getter @Setter private int borderColor = 0x000000;

}
