package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import java.util.List;

@Accessors(chain=true)
public class GameBoardSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameBoardSettings";

    @Getter @Setter private int length;
    @Getter @Setter private int width;
    @Getter @Setter private boolean wrapLength;
    @Getter @Setter private boolean wrapWidth;

    @Getter @Setter private List<BoardCellLocation> blocked;

}
