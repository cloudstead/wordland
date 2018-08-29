package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.collection.NameAndValue;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @Accessors(chain=true)
public class GameBoardPalette {

    private static final String ANY_UNASSIGNED = "*";

    public static final int DEFAULT_BLANK_RGB = 0xffffff;
    public static final int DEFAULT_CURRENT_PLAYER_RGB = 0xff0000;

    @Getter @Setter private String blankColor;
    @Getter @Setter private String currentPlayerId;
    @Getter @Setter private String currentPlayerColor;
    @Getter @Setter private NameAndValue[] playerColors;

    @JsonIgnore public int getBlankColorRgb () { return parseColor(blankColor, DEFAULT_BLANK_RGB); }

    @Getter(lazy=true) private final Map<String, String> colorsByPlayer = initColorsByPlayer();

    public GameBoardPalette(GameBoardPalette other) { copy(this, other); }

    public static GameBoardPalette defaultPalette(String currentPlayerId) {
        return json(stream2string("palette/default_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    private Map<String, String> initColorsByPlayer() {
        final Map<String, String> map = new HashMap<>();
        if (!empty(playerColors)) {
            for (NameAndValue color : playerColors) {
                if (!color.getName().equals(ANY_UNASSIGNED)) map.put(color.getName(), color.getValue());
            }
        }
        return map;
    }

    public int rgbFor(GameTileState tile) {
        if (tile == null || !tile.hasOwner()) return getBlankRgb();
        if (currentPlayerColor != null && tile.getOwner().equals(currentPlayerId)) {
            return parseColor(currentPlayerColor, DEFAULT_CURRENT_PLAYER_RGB);
        }
        final String colorString = getColorsByPlayer().get(tile.getOwner());
        if (!empty(colorString)) return parseColor(colorString, nextRandomColor(tile.getOwner()));
        return randomColor();
    }

    private int nextRandomColor(String player) {
        if (!empty(playerColors)) {
            String value = null;
            for (NameAndValue color : playerColors) {
                if (color.getName().equals(ANY_UNASSIGNED)) {
                    value = color.getValue();
                    color.setName(player);
                }
            }
            if (value != null) {
                return parseColor(value, randomColor());
            } else {
                return randomColor();
            }
        }
        return randomColor();
    }

    public static int randomColor() { return RandomUtils.nextInt(0x000000, 0xffffff); }

    @Getter(lazy=true) private final int blankRgb = parseColor(blankColor, DEFAULT_BLANK_RGB);

    private int parseColor(String colorString, int defaultRgb) {
        if (empty(colorString)) return defaultRgb;
        if (colorString.startsWith("0x")) return Integer.parseInt(colorString.substring(2), 16);
        if (colorString.startsWith("#")) return Integer.parseInt(colorString.substring(1), 16);
        return defaultRgb;
    }
}
