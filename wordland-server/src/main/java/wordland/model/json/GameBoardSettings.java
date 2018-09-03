package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

@Accessors(chain=true)
public class GameBoardSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameBoardSettings";

    @Getter @Setter private Integer length;
    public boolean hasLength () { return length != null; }
    public boolean infiniteLength () { return !hasLength(); }

    @Getter @Setter private Integer width;
    public boolean hasWidth () { return width != null; }
    public boolean infiniteWidth () { return !hasWidth(); }

    public boolean infinite () { return infiniteWidth() || infiniteLength(); }

}
