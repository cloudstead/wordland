package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import wordland.ApiConstants;
import wordland.model.*;

@Accessors(chain=true)
public class GameRoomSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameRoomSettings";

    @Getter @Setter private SymbolSet symbolSet;
    @Getter @Setter private SymbolDistribution defaultDistribution;
    @Getter @Setter private PointSystem pointSystem;
    @Getter @Setter private GameBoard board;
    @Getter @Setter private GameDictionary dictionary;

    @Getter @Setter private TeamPlayMode teamPlayMode;
    @Getter @Setter private int maxPlayers = 100;
    @Getter @Setter private Integer maxLetterDistance;
    public boolean hasMaxLetterDistance () { return maxLetterDistance != null; }

    @Getter @Setter private MissedTurnPolicy missedTurnPolicy;
    @Getter @Setter private BonusPolicy[] bonusPolicies;

    public String symbolSetName() {
        return symbolSet != null && symbolSet.getName() != null ? symbolSet.getName() : ApiConstants.STANDARD;
    }

    public String defaultDistributionName() {
        return defaultDistribution != null && defaultDistribution.getName() != null ? defaultDistribution.getName() : ApiConstants.STANDARD;
    }

    public String pointSystemName() {
        return pointSystem != null && pointSystem.getName() != null ? pointSystem.getName() : ApiConstants.STANDARD;
    }

    public String dictionaryName() {
        return dictionary != null && dictionary.getName() != null ? dictionary.getName() : ApiConstants.STANDARD;
    }

    public String boardName() {
        return board != null && board.getName() != null ? board.getName() : ApiConstants.STANDARD;
    }
}
