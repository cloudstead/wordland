package wordland.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.validation.HasValue;
import wordland.model.game.GameStateChangeType;
import wordland.model.game.score.PlayScore;

import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;

@NoArgsConstructor @Accessors(chain=true)
public class GameRuntimeEvent {

    @HasValue(message="err.id.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.id.length")
    @Getter @Setter private String id;

    @HasValue(message="err.clientId.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.clientId.length")
    @Getter @Setter private String clientId;
    public boolean hasClientId () { return clientId != null && clientId.trim().length() >= 36; }

    @HasValue(message="err.apiToken.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.apiToken.length")
    @Getter @Setter private String apiToken;

    @HasValue(message="err.room.empty")
    @Size(max=Identifiable.UUID_MAXLEN, message="err.room.length")
    @Getter @Setter private String room;

    @Getter @Setter private GameStateChangeType stateChange;
    public boolean hasStateChange () { return stateChange != null; }

    @Getter @Setter private String word;
    public boolean hasWord () { return word != null && word.length() > 0; }

    @Getter @Setter private PlayedTile[] tiles;
    public boolean hasTiles () { return tiles != null && tiles.length > 0; }

    public String getTilesJson () { return json(tiles); }
    public GameRuntimeEvent setTilesJson(String json) { return setTiles(json(json, PlayedTile[].class)); }

    public String tileCoordinates() {
        final StringBuilder b = new StringBuilder();
        if (hasTiles()) {
            for (PlayedTile tile : tiles) {
                if (b.length() > 0) b.append(", ");
                b.append("{").append(tile.getSymbol()).append(": ").append(tile.getX()).append(", ").append(tile.getY()).append("}");
            }
        }
        return b.toString();
    }

    @Getter @Setter private PlayScore score;
    public boolean hasScore () { return score != null && score.getTotal(id) > 0; }

    @Getter @Setter private String[] winners;
    public boolean hasWinners () { return !empty(winners); }

}
