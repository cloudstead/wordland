package wordland.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Slf4j
public class TileImage {

    public static final float SYMBOL_FILL_RATIO = 0.7f;

    protected Font defaultFont(int fontSize) { return new Font(Font.SANS_SERIF, Font.PLAIN, fontSize); }

    @Getter private TileImageSettings settings;

    public byte[] png () {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        png(out);
        return out.toByteArray();
    }

    public void png (OutputStream out) {
        final int tileSize = settings.getSize();
        final BufferedImage bufferedImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = bufferedImage.createGraphics();

        // fill background
        g2.setColor(new Color(settings.getBgColor()));
        g2.fillRect(0, 0, tileSize, tileSize);

        // draw border
        g2.setStroke(new BasicStroke(settings.getBorder()));
        g2.setColor(new Color(settings.getBorderColor()));
        g2.draw(new Rectangle(tileSize-1, tileSize-1));

        // determine font and size
        int fontSize = (int) (settings.getSize() * SYMBOL_FILL_RATIO);
        final String fontName = settings.getFont();

        //noinspection MagicConstant -- settings.getFontStyleConstant returns a Font constant
        g2.setFont(empty(fontName)
                ? defaultFont(fontSize)
                : new Font(fontName, settings.getFontStyleConstant(), fontSize)
        );

        // set anti-aliasing
        if (settings.isAntiAlias()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }

        // determine symbol
        final String symbol = settings.symbol();

        // determine x/y position based on pixel dimensions of rendered string
        final FontMetrics fm = g2.getFontMetrics(g2.getFont());
        final Rectangle2D stringBounds = fm.getStringBounds(symbol, g2);
        //noinspection UnnecessaryLocalVariable -- makes life simpler
        final double tileSizeDouble = tileSize;
        final float xpos = (float) ((tileSizeDouble - stringBounds.getWidth())/2.0d);
        final float ypos = (float) (stringBounds.getHeight() - ( (tileSizeDouble - stringBounds.getHeight())/3.0d) );

        // draw symbol
        g2.setColor(new Color(settings.getFgColor()));
        g2.drawString(symbol, xpos, ypos);

        // return PNG bytes
        try {
            ImageIO.write(bufferedImage, "png", out);
        } catch (Exception e) {
            die("png: "+e, e);
        }
    }

}
