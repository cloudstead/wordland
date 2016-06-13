package wordland.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GeneratePlayerCssOptions extends BaseMainOptions {

    public static final String USAGE_FILE = "Output file. Default is stdout.";
    public static final String OPT_FILE = "-f";
    public static final String LONGOPT_FILE= "--file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE)
    @Getter @Setter private File file;
    public boolean hasFile () { return file != null; }

    public OutputStream getOutputStream() throws IOException {
        if (hasFile()) return new FileOutputStream(getFile());
        return System.out;
    }
}
