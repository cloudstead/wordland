package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import wordland.model.PointSystem;
import wordland.model.SymbolDistribution;
import wordland.model.SymbolSet;

@Accessors(chain=true)
public class GameRoomSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameRoomSettings";

    @Getter @Setter private SymbolSet symbolSet;
    @Getter @Setter private SymbolDistribution defaultDistribution;
    @Getter @Setter private PointSystem pointSystem;
    @Getter @Setter private GameBoardSettings boardSettings;

    @Getter @Setter private int maxPlayers;

}
