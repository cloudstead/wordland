package wordland.model.support;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.validation.HasValue;

import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class GameRoomJoinRequest {

    @HasValue(message="err.clientId.empty")
    @Size(min=36, max=100, message="err.clientId.length")
    @Getter @Setter private String clientId;
    public boolean hasClientId () { return !empty(clientId); }

    @Size(max=100, message="err.name.length")
    @Getter @Setter private String name;
    public boolean hasName () { return !empty(name); }

    @Size(max=100, message="err.team.length")
    @Getter @Setter private String team;
    public boolean hasTeam () { return !empty(team); }

}
