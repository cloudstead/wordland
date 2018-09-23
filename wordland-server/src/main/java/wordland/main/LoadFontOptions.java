package wordland.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.Base64;
import org.kohsuke.args4j.Option;
import wordland.model.SymbolFont;

import java.io.File;
import java.io.IOException;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.basename;
import static org.cobbzilla.util.io.FileUtil.chopSuffix;

public class LoadFontOptions extends WordlandApiOptionsBase {

    public static final String USAGE_NAME = "Name of the font. Default is the basename of the file, without the .ttf suffix";
    public static final String OPT_NAME = "-n";
    public static final String LONGOPT_NAME= "--name";
    @Option(name=OPT_NAME, aliases=LONGOPT_NAME, usage=USAGE_NAME)
    @Getter @Setter private String name;

    public boolean hasName () { return !empty(name); }

    public String name () { return hasName() ? getName() : chopSuffix(basename(ttfFile.getName())); }

    public static final String USAGE_TTF_FILE = "Path to TTF file for font";
    public static final String OPT_TTF_FILE = "-t";
    public static final String LONGOPT_TTF_FILE= "--ttf";
    @Option(name=OPT_TTF_FILE, aliases=LONGOPT_TTF_FILE, usage=USAGE_TTF_FILE, required=true)
    @Getter @Setter private File ttfFile;

    public static final String USAGE_UPDATE = "Update the font instead of creating a new one. Default is false";
    public static final String OPT_UPDATE = "-u";
    public static final String LONGOPT_UPDATE= "--update";
    @Option(name=OPT_UPDATE, aliases=LONGOPT_UPDATE, usage=USAGE_UPDATE)
    @Getter @Setter private boolean update = false;

    public SymbolFont getFontRequest() throws IOException {
        return (SymbolFont) new SymbolFont()
                .setBase64ttf(Base64.encodeFromFile(ttfFile))
                .setName(name());
    }
}
