package wordland.main;

import lombok.Cleanup;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class GeneratePlayerCss extends MainBase<GeneratePlayerCssOptions> {

    public static final String[] color_set1 = {
            "lightcoral", "lightsalmon", "lightgreen", "mediumpurple", "khaki", "gold", "lightpink", "forestgreen"
    };
    public static final String[] color_set2 = {
            "red", "orangered", "green", "purple", "chocolate", "yellow", "deeppink", "darkgreen"
    };

    public static void main (String[] args) { main(GeneratePlayerCss.class, args); }

    @Override protected void run() throws Exception {
        final GeneratePlayerCssOptions options = getOptions();
        @Cleanup final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(options.getOutputStream()));
        for (int i=0; i<color_set1.length; i++) {
            final String color1 = color_set1[i];
            final String color2 = color_set2[i];
            writeCssBlock(writer, i, color1, color2);
        }
        for (int i=0; i<color_set1.length; i++) {
            final String color1 = color_set1[i];
            final String color2 = color_set2[i];
            writeCssBlock(writer, i+color_set1.length, color2, color1);
        }
        writer.flush();
    }

    private void writeCssBlock(BufferedWriter writer, int i, String color1, String color2) throws IOException {
        writer.write(".playerCell_"+i+" {\n" +
                "    background-color: "+color1+";\n" +
                "    width: 18px;\n" +
                "    height: 14px;\n" +
                "}\n" +
                ".playerLog_"+i+" {\n" +
                "    border: 2px solid "+color2+";\n" +
                "    background-color: "+color1+";\n" +
                "    margin: 3px;\n" +
                "    padding-left: 5px;\n" +
                "    padding-right: 5px;\n" +
                "}\n");
    }
}
