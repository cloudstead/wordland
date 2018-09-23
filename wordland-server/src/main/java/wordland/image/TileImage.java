package wordland.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @AllArgsConstructor @Slf4j
public class TileImage {

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
        g2.draw(new Rectangle(tileSize, tileSize));

        // draw symbol
        g2.setColor(new Color(settings.getFgColor()));
        //g2.setFont(new Font());
        g2.drawString(settings.getSymbol().toUpperCase(), 0.2f*tileSize, 0.8f*tileSize);

        // return PNG bytes
        try {
            ImageIO.write(bufferedImage, "png", out);
        } catch (Exception e) {
            die("png: "+e, e);
        }
    }

}
