package wordland.model.support;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class GameRoomJoinRequest {

    @Size(max=100, message="err.name.length")
    @Getter @Setter private String name;
    public boolean hasName () { return !empty(name); }

    @Size(max=100, message="err.team.length")
    @Getter @Setter private String team;
    public boolean hasTeam () { return !empty(team); }

}
