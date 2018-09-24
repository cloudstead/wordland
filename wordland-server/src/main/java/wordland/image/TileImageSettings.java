package wordland.image;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.awt.*;

import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Accessors(chain=true) @EqualsAndHashCode
public class TileImageSettings {

    @Getter @Setter private String symbol;
    @Getter @Setter private String font;
    @Getter @Setter private String fontStyle;
    @Getter @Setter private boolean antiAlias = true;
    @Getter @Setter private SymbolCaseMode caseMode = SymbolCaseMode.upper;

    public String symbol() { return caseMode.apply(getSymbol()); }

    @Getter @Setter private int size = 64;
    @Getter @Setter private int border = 1;
    @Getter @Setter private int fgColor = 0x000000;
    @Getter @Setter private int bgColor = 0xffffff;
    @Getter @Setter private int borderColor = 0x000000;

    public int getFontStyleConstant() {
        if (fontStyle == null) return Font.PLAIN;
        switch (fontStyle.toLowerCase()) {
            case "plain": return Font.PLAIN;
            case "bold": return Font.BOLD;
            case "italic": return Font.ITALIC;
            case "bold-italic": return Font.BOLD | Font.ITALIC;
        }
        throw invalidEx("err.fontStyle.invalid");
    }

}
