package wordland.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;
import wordland.image.TileImageSettings;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.graphics.ColorUtil.parseRgb;

public class TileImageMainOptions extends BaseMainOptions {

    public static final String USAGE_OUTPUT = "Output file. Default is symbol.png, or for entire tile sets, a new temp directory containing a png file for each symbol";
    public static final String OPT_OUTPUT = "-o";
    public static final String LONGOPT_OUTPUT= "--out";
    @Option(name=OPT_OUTPUT, aliases=LONGOPT_OUTPUT, usage=USAGE_OUTPUT)
    @Getter @Setter private File output = null;

    public boolean hasOutput () { return getOutput() != null; }

    public static final String USAGE_SYMBOL = "Symbol to write on tile";
    public static final String OPT_SYMBOL = "-S";
    public static final String LONGOPT_SYMBOL= "--symbol";
    @Option(name=OPT_SYMBOL, aliases=LONGOPT_SYMBOL, usage=USAGE_SYMBOL)
    @Getter @Setter private String symbol;

    public boolean hasSymbol() { return !empty(getSymbol()); }

    public static final String USAGE_SYMBOL_SET = "Create a tile for every symbol in the named symbol set";
    public static final String OPT_SYMBOL_SET = "-T";
    public static final String LONGOPT_SYMBOL_SET= "--symbol-set";
    @Option(name=OPT_SYMBOL_SET, aliases=LONGOPT_SYMBOL_SET, usage=USAGE_SYMBOL_SET)
    @Getter @Setter private String symbolSet;

    public boolean hasSymbolSet() { return !empty(getSymbolSet()); }

    public static final String USAGE_SIZE = "Size (width and height) of tile in pixels. Default is 64";
    public static final String OPT_SIZE = "-s";
    public static final String LONGOPT_SIZE= "--size";
    @Option(name=OPT_SIZE, aliases=LONGOPT_SIZE, usage=USAGE_SIZE)
    @Getter @Setter private int size = 64;

    public static final String USAGE_BORDER = "Border size in pixels. Default is 1";
    public static final String OPT_BORDER = "-b";
    public static final String LONGOPT_BORDER= "--border";
    @Option(name=OPT_BORDER, aliases=LONGOPT_BORDER, usage=USAGE_BORDER)
    @Getter @Setter private int border = 1;

    public static final String USAGE_BORDER_COLOR = "Border color. Default is black";
    public static final String OPT_BORDER_COLOR = "-O";
    public static final String LONGOPT_BORDER_COLOR= "--border-color";
    @Option(name=OPT_BORDER_COLOR, aliases=LONGOPT_BORDER_COLOR, usage=USAGE_BORDER_COLOR)
    @Getter @Setter private String borderColor = "0x000000";

    public static final String USAGE_FOREGROUND_COLOR = "Foreground color. Default is black";
    public static final String OPT_FOREGROUND_COLOR = "-F";
    public static final String LONGOPT_FOREGROUND_COLOR= "--foreground-color";
    @Option(name=OPT_FOREGROUND_COLOR, aliases=LONGOPT_FOREGROUND_COLOR, usage=USAGE_FOREGROUND_COLOR)
    @Getter @Setter private String foregroundColor = "0x000000";

    public static final String USAGE_BACKGROUND_COLOR = "Background color. Default is white";
    public static final String OPT_BACKGROUND_COLOR = "-B";
    public static final String LONGOPT_BACKGROUND_COLOR= "--background-color";
    @Option(name=OPT_BACKGROUND_COLOR, aliases=LONGOPT_BACKGROUND_COLOR, usage=USAGE_BACKGROUND_COLOR)
    @Getter @Setter private String backgroundColor = "0xffffff";

    public TileImageSettings getSettings() {
        return new TileImageSettings()
                .setSymbol(getSymbol())
                .setSize(getSize())
                .setBorder(getBorder())
                .setBorderColor(parseRgb(getBorderColor()))
                .setFgColor(parseRgb(getForegroundColor()))
                .setBgColor(parseRgb(getBackgroundColor()));
    }
}
