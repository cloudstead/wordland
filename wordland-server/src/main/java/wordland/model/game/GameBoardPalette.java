package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @Accessors(chain=true)
public class GameBoardPalette {

    private static final String ANY_UNASSIGNED = "*";

    public static final int DEFAULT_BLANK_RGB = 0xffffff;
    public static final int DEFAULT_BLANK_ANSI = 15;

    public static final int DEFAULT_CURRENT_PLAYER_RGB = 0xff0000;
    public static final int DEFAULT_CURRENT_PLAYER_ANSI = 9;

    @Getter @Setter private String blankColor;
    @Getter @Setter private String currentPlayerId;
    @Getter @Setter private String currentPlayerColor;

    @Getter @Setter private NameAndValue[] playerColors;
    public void addPlayerColor (String player, String color) {
        playerColors = ArrayUtil.append(playerColors, new NameAndValue(player, color));
    }

    private Set<Integer> usedColors = new HashSet<>();

    @JsonIgnore public int getCurrentPlayerColorRgb() { return parseColor(currentPlayerColor, DEFAULT_CURRENT_PLAYER_RGB); }
    @JsonIgnore public int getCurrentPlayerColorAnsi() { return parseAnsi(currentPlayerColor, DEFAULT_CURRENT_PLAYER_ANSI); }
    @JsonIgnore public int getBlankColorRgb () { return parseColor(blankColor, DEFAULT_BLANK_RGB); }
    @JsonIgnore public int getBlankColorAnsi () { return parseAnsi(blankColor, DEFAULT_BLANK_ANSI); }

    @Getter(lazy=true) private final Map<String, String> colorsByPlayer = initColorsByPlayer();
    private Map<String, String> initColorsByPlayer() {
        final Map<String, String> map = new HashMap<>();
        if (!empty(playerColors)) {
            for (NameAndValue color : playerColors) {
                if (!color.getName().equals(ANY_UNASSIGNED)) map.put(color.getName(), color.getValue());
            }
        }
        return map;
    }

    public GameBoardPalette(GameBoardPalette other) { copy(this, other); }

    public static GameBoardPalette defaultPalette(String currentPlayerId) {
        return json(stream2string("palette/default_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public static GameBoardPalette defaultAnsiPalette(String currentPlayerId) {
        return json(stream2string("palette/default_ansi_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public int rgbFor(GameTileState tile) {
        int color = _rgbFor(tile);
        usedColors.add(color);
        return color;
    }
    public int _rgbFor(GameTileState tile) {
        if (tile == null || !tile.hasOwner()) return getBlankColorRgb();
        if (currentPlayerColor != null && tile.getOwner().equals(currentPlayerId)) {
            return parseColor(currentPlayerColor, DEFAULT_CURRENT_PLAYER_RGB);
        }
        final String colorString = getColorsByPlayer().get(tile.getOwner());
        if (!empty(colorString)) return parseColor(colorString, nextRandomColor(tile.getOwner()));
        return randomColor();
    }

    public int ansiColorFor(GameTileState tile) {
        int color = _ansiColorFor(tile);
        usedColors.add(color);
        return color;
    }

    public int _ansiColorFor(GameTileState tile) {
        if (tile == null || !tile.hasOwner()) return Integer.parseInt(getBlankColor());
        if (currentPlayerColor != null) {
            if (tile.getOwner().equals(currentPlayerId)) return Integer.parseInt(currentPlayerColor, DEFAULT_CURRENT_PLAYER_ANSI);
            return DEFAULT_CURRENT_PLAYER_ANSI;
        }
        final String colorString = getColorsByPlayer().get(tile.getOwner());
        if (!empty(colorString)) return Integer.parseInt(colorString);
        return nextRandomAnsiColor(tile.getOwner());
    }

    private int nextRandomColor(String player) {
        if (!empty(playerColors)) {
            String value = null;
            for (NameAndValue color : playerColors) {
                if (color.getName().equals(ANY_UNASSIGNED)) {
                    value = color.getValue();
                    color.setName(player);
                    break;
                }
            }
            return value != null ? parseColor(value, randomColor()) : randomColor();
        }
        return randomColor();
    }

    private int nextRandomAnsiColor(String player) {
        if (!empty(playerColors)) {
            String value = null;
            for (NameAndValue color : playerColors) {
                if (color.getName().equals(ANY_UNASSIGNED)) {
                    value = color.getValue();
                    color.setName(player);
                    break;
                }
            }
            return value != null ? Integer.parseInt(value) : randomColor();
        }
        return randomAnsiColor();
    }

    public int randomColor() {
        int val;
        do {
            val = RandomUtils.nextInt(0x000000, 0xffffff);
        } while (val == getBlankColorRgb() || val == getCurrentPlayerColorRgb() || usedColors.contains(val));
        return val;
    }

    public int randomAnsiColor() {
        int val;
        do {
            val = RandomUtils.nextInt(1, 254);
        }
        while (val == getBlankColorAnsi() || val == getCurrentPlayerColorAnsi() || usedColors.contains(val));
        return val;
    }

    private int parseAnsi(String color, int defaultColor) {
        try {
            return Integer.parseInt(color);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private int parseColor(String colorString, int defaultRgb) {
        if (empty(colorString)) return defaultRgb;
        if (colorString.startsWith("0x")) return Integer.parseInt(colorString.substring(2), 16);
        if (colorString.startsWith("#")) return Integer.parseInt(colorString.substring(1), 16);
        return defaultRgb;
    }
}
