package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;
import org.hibernate.annotations.Type;
import wordland.model.game.GameState;
import wordland.model.game.GameStateStorageService;
import wordland.model.json.GameRoomSettings;

import javax.persistence.Column;
import javax.persistence.Entity;

import static wordland.ApiConstants.GAME_ROOMS_ENDPOINT;

@ECType(root=true)
@ECTypeURIs(baseURI=GAME_ROOMS_ENDPOINT)
@Entity @NoArgsConstructor @Accessors(chain=true)
public class GameRoom extends NamedIdentityBase {

    public GameRoom (GameRoom other) { super(other.getName()); setSettings(other.getSettings()); }

    public GameRoom (String name) { super(name); }

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSettings(((GameRoom) other).getSettings());
    }

    @Type(type=GameRoomSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private GameRoomSettings settings;

    public GameState initializeBoard(GameStateStorageService stateStorage) {
        return new GameState(getSettings(), stateStorage);
    }

}
