package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.Account;
import wordland.model.support.GameRoomJoinRequest;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static wordland.WordConstants.ADJECTIVES;
import static wordland.WordConstants.FRUITS;

@NoArgsConstructor @Accessors(chain=true)
public class GamePlayer {

    @Getter @Setter private String account;
    @Getter @Setter private String name;
    @Getter @Setter private String team;

    @Getter @Setter private long ctime = now();
    @JsonIgnore public long getAge () { return now() - ctime; }

    @Getter @Setter private long lastMove = 0;
    @JsonIgnore public long getTimeSinceLastMove () { return now() - lastMove; }

    public GamePlayer(Account account, GameRoomJoinRequest request) {
        this.account = account == null ? uuid() : account.getUuid();
        this.name = request.hasName() ? request.getName() : randomName();
        this.team = request.getTeam();
    }

    private String randomName() {
        return (pickRandom(ADJECTIVES) + " " + pickRandom(FRUITS)).toLowerCase();
    }

}
