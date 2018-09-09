package wordland.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import wordland.model.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;
import static wordland.ApiConstants.STANDARD;

@Accessors(chain=true)
public class GameRoomSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_GameRoomSettings";

    @Getter @Setter private SymbolSet symbolSet;
    @Getter @Setter private SymbolDistribution symbolDistribution;
    @Getter @Setter private PointSystem pointSystem;
    @Getter @Setter private GameBoard board;
    @Getter @Setter private GameDictionary dictionary;

    @Getter @Setter private TeamPlayMode teamPlayMode;

    @Getter @Setter private Integer minPlayersToStart = 2;
    public boolean hasMinPlayersToStart () { return minPlayersToStart != null; }

    @Getter @Setter private Integer maxPlayers = 100;
    public boolean hasMaxPlayers () { return maxPlayers != null; }

    @Getter @Setter private Integer maxLetterDistance;
    public boolean hasMaxLetterDistance () { return maxLetterDistance != null; }

    @Getter @Setter private TurnPolicy[] turnPolicies;
    public boolean hasTurnPolicies () { return !empty(turnPolicies); }

    @JsonIgnore public TurnPolicy getRoundRobinPolicy () {
        if (turnPolicies != null) {
            for (TurnPolicy p : turnPolicies) {
                if (p.getType() == TurnPolicyType.round_robin) return p;
            }
        }
        return null;
    }
    public boolean hasRoundRobinPolicy () { return getRoundRobinPolicy() != null; }

    @Getter @Setter private MissedTurnPolicy missedTurnPolicy;
    @Getter @Setter private BonusPolicy[] bonusPolicies;
    @Getter @Setter private WinCondition[] winConditions;
    public boolean hasWinConditions () { return !empty(getWinConditions()); }

    @Getter @Setter private String maxWaitBeforeBotsJoin;
    public boolean hasMaxWaitBeforeBotsJoin () { return maxWaitBeforeBotsJoin != null; }

    @JsonIgnore public Long getMillisBeforeBotsJoin () {
        return hasMaxWaitBeforeBotsJoin() ? parseDuration(maxWaitBeforeBotsJoin) : null;
    }

    public String symbolSetName() {
        return symbolSet != null && symbolSet.getName() != null ? symbolSet.getName() : STANDARD;
    }

    public String symbolDistributionName() {
        return symbolDistribution != null && symbolDistribution.getName() != null ? symbolDistribution.getName() : STANDARD;
    }

    public String pointSystemName() {
        return pointSystem != null && pointSystem.getName() != null ? pointSystem.getName() : STANDARD;
    }

    public String dictionaryName() {
        return dictionary != null && dictionary.getName() != null ? dictionary.getName() : STANDARD;
    }

    public String boardName() {
        return board != null && board.getName() != null ? board.getName() : STANDARD;
    }

    public void mergeSettings(GameRoomSettings other) { ReflectionUtil.copy(this, other); }

}
