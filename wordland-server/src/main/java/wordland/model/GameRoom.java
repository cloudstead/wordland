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
import wordland.model.game.RoomState;
import wordland.model.json.GameRoomSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import java.util.regex.Pattern;

import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static wordland.ApiConstants.GAME_ROOMS_ENDPOINT;

@ECType(root=true)
@ECTypeURIs(baseURI=GAME_ROOMS_ENDPOINT)
@Entity @NoArgsConstructor @Accessors(chain=true)
public class GameRoom extends NamedIdentityBase {

    public GameRoom (GameRoom other) {
        super(other.getName());
        setTemplate(other.isTemplate());
        setSettings(other.getSettings());
    }

    public GameRoom (String name) { super(name); }

    public static final Pattern GAME_ROOM_NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]+[A-Za-z0-9]");

    @Override public NamedIdentityBase setName(String name) {
        // validate nme
        if (!GAME_ROOM_NAME_PATTERN.matcher(name).matches()) {
            throw invalidEx("err.roomName.invalid");
        }
        return super.setName(name);
    }

    @Column(updatable=false)
    @Getter @Setter private String accountOwner;

    @Getter @Setter private boolean template;

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSettings(((GameRoom) other).getSettings());
    }

    @Type(type=GameRoomSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private GameRoomSettings settings;

    public GameRoom mergeSettings(GameRoomSettings other) { settings.mergeSettings(other); return this; }

    public GameState init(GameStateStorageService stateStorage) {
        return new GameState(this, stateStorage);
    }

    @Transient @Getter @Setter private RoomState roomState;

}
