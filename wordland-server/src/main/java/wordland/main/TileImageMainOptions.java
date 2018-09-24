package wordland.main;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;
import wordland.image.SymbolCaseMode;
import wordland.image.TileImageSettings;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
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

    public static final String USAGE_FONT = "Name of the font to use, or path to a true-type font. Default is sans-serif";
    public static final String OPT_FONT = "-f";
    public static final String LONGOPT_FONT= "--font";
    @Option(name=OPT_FONT, aliases=LONGOPT_FONT, usage=USAGE_FONT)
    @Getter @Setter private String font;
    public boolean hasFont() { return !empty(getFont()); }

    public static final String USAGE_FONT_STYLE = "Name of the font style. Default is plain. Can be plain, bold, italic, or bold-italic";
    public static final String OPT_FONT_STYLE = "-t";
    public static final String LONGOPT_FONT_STYLE= "--font-style";
    @Option(name=OPT_FONT_STYLE, aliases=LONGOPT_FONT_STYLE, usage=USAGE_FONT_STYLE)
    @Getter @Setter private String fontStyle = "plain";

    public static final String USAGE_NO_ANTI_ALIAS = "If set, disable anti-aliasing. Anti-aliasing is enabled by default.";
    public static final String OPT_NO_ANTI_ALIAS = "-a";
    public static final String LONGOPT_NO_ANTI_ALIAS= "--no-anti-alias";
    @Option(name=OPT_NO_ANTI_ALIAS, aliases=LONGOPT_NO_ANTI_ALIAS, usage=USAGE_NO_ANTI_ALIAS)
    @Getter @Setter private boolean noAntiAlias = false;

    public static final String USAGE_CASE_MODE = "Case conversion mode for symbol(s). Default is to convert symbols to uppercase.";
    public static final String OPT_CASE_MODE = "-c";
    public static final String LONGOPT_CASE_MODE= "--case-mode";
    @Option(name=OPT_CASE_MODE, aliases=LONGOPT_CASE_MODE, usage=USAGE_CASE_MODE)
    @Getter @Setter private SymbolCaseMode caseMode = SymbolCaseMode.upper;

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
                .setFont(getFont())
                .setFontStyle(getFontStyle())
                .setAntiAlias(!isNoAntiAlias())
                .setCaseMode(getCaseMode())
                .setSize(getSize())
                .setBorder(getBorder())
                .setBorderColor(parseRgb(getBorderColor()))
                .setFgColor(parseRgb(getForegroundColor()))
                .setBgColor(parseRgb(getBackgroundColor()));
    }

    public void registerFont() throws Exception {
        if (hasFont()) {
            final GraphicsEnvironment ge = getLocalGraphicsEnvironment();
            Font font = null;
            for (Font f : ge.getAllFonts()) {
                if (f.getName().equalsIgnoreCase(getFont())) {
                    font = f;
                    break;
                }
            }
            if (font == null) {
                final File fontFile = new File(getFont());
                if (!fontFile.exists()) die("ERROR: registerFont: not a valid font name or path to ttf file: "+getFont());

                @Cleanup final FileInputStream ttf = new FileInputStream(fontFile);
                font = Font.createFont(Font.TRUETYPE_FONT, ttf);

                if (!ge.registerFont(font)) out("WARNING: registerFont failed for font " + getFont());
                setFont(font.getName());
            }
        } else {
            setFont("SansSerif");
        }
    }
}
