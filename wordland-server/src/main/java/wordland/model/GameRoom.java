package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.hibernate.annotations.Type;
import wordland.model.game.GameState;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.json.SymbolDistributionSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Iterator;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class GameRoom extends NamedIdentityBase {

    public GameRoom (GameRoom other) { super(other.getName()); setSettings(other.getSettings()); }

    public GameRoom (String name) { super(name); }

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSettings(((GameRoom) other).getSettings());
    }

    @Type(type=GameRoomSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private GameRoomSettings settings;

    public GameState randomizeTiles() {

        final SymbolDistributionSettings distribution = getSettings().getDefaultDistribution().getSettings();
        final GameBoardSettings board = getSettings().getBoard().getSettings();

        final Iterator<String> picker = distribution.getPicker();

        final GameState gameState = new GameState(board.getLength(), board.getWidth(), getSettings().getMaxPlayers());

        for (int x=0; x<board.getLength(); x++) {
            for (int y=0; y<board.getWidth(); y++) {
                gameState.setTileSymbol(x, y, picker.next());
            }
        }

        return gameState;
    }
}
