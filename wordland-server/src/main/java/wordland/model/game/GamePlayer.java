package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.support.AccountSession;
import wordland.model.support.GameRoomJoinRequest;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.daemon.ZillaRuntime.pickRandom;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static wordland.WordConstants.ADJECTIVES;
import static wordland.WordConstants.FRUITS;

@NoArgsConstructor @Accessors(chain=true)
public class GamePlayer {

    public static final GamePlayer UNKNOWN_PLAYER = new GamePlayer().setName("??");

    public GamePlayer(AccountSession session, GameRoomJoinRequest request) {
        this.id = session.getId();
        this.apiToken = session.getApiToken();
        this.name = request.hasName() ? request.getName() : session.getName();
        this.team = request.getTeam();
    }

    public static final String[] PUBLIC_FIELDS = {"id", "name", "team", "bot"};

    public GamePlayer publicView() {
        // only the fields we choose here are visible
        final GamePlayer player = new GamePlayer();
        copy(player, this, PUBLIC_FIELDS);
        return player;
    }

    @Getter @Setter private String id;
    @Getter @Setter private String apiToken;
    @Getter @Setter private String name;
    @Getter @Setter private String team;
    @Getter @Setter private Boolean bot;
    @JsonIgnore public boolean bot () { return bot != null && bot; }

    @Getter @Setter private long ctime = now();
    @JsonIgnore public long getAge () { return now() - ctime; }

    @Getter @Setter private long lastMove = 0;
    @JsonIgnore public long getTimeSinceLastMove () { return now() - lastMove; }

    private String randomName() {
        return (pickRandom(ADJECTIVES) + " " + pickRandom(FRUITS)).toLowerCase();
    }

    @JsonIgnore public GamePlayerCredentials getCredentials() { return new GamePlayerCredentials(name, team, id, apiToken); }

}
