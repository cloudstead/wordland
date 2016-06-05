package wordland.model.support;

import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class GameRoomJoinRequest {

    @Getter @Setter private String name;
    public boolean hasName () { return !empty(name); }

    @Getter @Setter private String team;
    public boolean hasTeam () { return !empty(team); }

}
