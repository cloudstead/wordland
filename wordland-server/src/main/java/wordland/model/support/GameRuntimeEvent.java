package wordland.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.validation.HasValue;
import wordland.model.game.GameStateChangeType;

import javax.validation.constraints.Size;

@NoArgsConstructor @Accessors(chain=true)
public class GameRuntimeEvent {

    @HasValue(message="err.id.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.id.length")
    @Getter @Setter private String id;

    @HasValue(message="err.clientId.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.clientId.length")
    @Getter @Setter private String clientId;
    public boolean hasClientId () { return clientId != null && clientId.trim().length() >= 36; }

    @HasValue(message="err.apiKey.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.apiKey.length")
    @Getter @Setter private String apiKey;

    @HasValue(message="err.room.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.room.length")
    @Getter @Setter private String room;

    @Getter @Setter private GameStateChangeType stateChange;
    public boolean hasStateChange () { return stateChange != null; }

    @Getter @Setter private String word;
    public boolean hasWord () { return word != null && word.length() > 0; }

    @Getter @Setter private PlayedTile[] tiles;
    public boolean hasTiles () { return tiles != null && tiles.length > 0; }

}
