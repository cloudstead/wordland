package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.Base64;

import java.io.IOException;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@NoArgsConstructor @Accessors(chain=true) @EqualsAndHashCode(exclude={"ctime", "base64image"})
public class GameBoardView {

    @Getter @Setter private long ctime = now();
    public boolean youngerThan (long age) { return now() - ctime < age; }

    @Getter @Setter private String room;
    @Getter @Setter private String base64image;
    @Getter @Setter private int imageWidth;
    @Getter @Setter private int imageHeight;
    @Getter @Setter private int tilesWidth;
    @Getter @Setter private int tilesHeight;
    @Getter @Setter private int x1;
    @Getter @Setter private int x2;
    @Getter @Setter private int y1;
    @Getter @Setter private int y2;
    @Getter @Setter private GameBoardPalette palette;
    @Getter @Setter private RoomState roomState;

    @JsonIgnore public byte[] getImage() throws IOException { return Base64.decode(base64image); }

    public void setImage(byte[] bytes) { setBase64image(Base64.encodeBytes(bytes)); }
}
