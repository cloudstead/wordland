package wordland.main;

import org.cobbzilla.util.io.TempDir;
import org.cobbzilla.util.main.BaseMain;
import wordland.ApiConstants;
import wordland.image.TileImage;
import wordland.image.TileImageSettings;
import wordland.model.SymbolSet;

import java.io.File;
import java.io.FileOutputStream;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.main.TileImageMainOptions.*;

public class TileImageMain extends BaseMain<TileImageMainOptions> {

    public static void main (String[] args) { main(TileImageMain.class, args); }

    @Override protected void run() throws Exception {
        final TileImageMainOptions options = getOptions();
        final TileImageSettings settings = options.getSettings();
        if (options.hasSymbol()) {
            if (options.hasSymbolSet()) die("Cannot specify both "+OPT_SYMBOL+"/"+LONGOPT_SYMBOL+" and "+OPT_SYMBOL_SET+"/"+LONGOPT_SYMBOL_SET);
            final TileImage tileImage = new TileImage(settings);
            final File file = options.hasOutput() ? options.getOutput() : new File(options.getSymbol()+".png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                tileImage.png(out);
            }
            out("tile image saved to "+abs(file));

        } else {
            final String setName = options.hasSymbolSet() ? options.getSymbolSet() : ApiConstants.STANDARD;

            // read SymbolSets from seed file
            final SymbolSet[] symbolSets = json(stream2string("seed/SymbolSet.json"), SymbolSet[].class);
            for (SymbolSet set : symbolSets) {
                if (set.getName().equals(setName)) {
                    tilesForSymbolSet(set);
                    return;
                }
            }
            err("symbol set not found: "+options.getSymbol());
        }
    }

    private void tilesForSymbolSet(SymbolSet set) throws Exception {
        final TileImageMainOptions options = getOptions();
        final File outDir = options.hasOutput() ? options.getOutput() : new TempDir();
        final TileImageSettings settings = options.getSettings();
        for (String symbol : set.getSymbols()) {
            try (FileOutputStream out = new FileOutputStream(new File(outDir, symbol+".png"))) {
                new TileImage(settings.setSymbol(symbol)).png(out);
            }
        }
        out("tile images saved to "+abs(outDir));
    }
}
