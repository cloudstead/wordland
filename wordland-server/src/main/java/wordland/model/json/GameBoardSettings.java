package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

@Accessors(chain=true)
public class GameBoardSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameBoardSettings";

    @Getter @Setter private int length;
    @Getter @Setter private int width;

}
