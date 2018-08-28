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
import static wordland.WordConstants.ADJECTIVES;
import static wordland.WordConstants.FRUITS;

@NoArgsConstructor @Accessors(chain=true)
public class GamePlayer {

    public GamePlayer(AccountSession session, GameRoomJoinRequest request) {
        this.id = session.getId();
        this.apiToken = session.getApiToken();
        this.name = request.hasName() ? request.getName() : session.getName();
        this.team = request.getTeam();
    }

    @Getter @Setter private String id;
    @Getter @Setter private String apiToken;
    @Getter @Setter private String name;
    @Getter @Setter private String team;

    @Getter @Setter private long ctime = now();
    @JsonIgnore public long getAge () { return now() - ctime; }

    @Getter @Setter private long lastMove = 0;
    @JsonIgnore public long getTimeSinceLastMove () { return now() - lastMove; }

    private String randomName() {
        return (pickRandom(ADJECTIVES) + " " + pickRandom(FRUITS)).toLowerCase();
    }

    @JsonIgnore public GamePlayerCredentials getCredentials() { return new GamePlayerCredentials(name, team, id, apiToken); }

}
