package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.getHexValue;

@NoArgsConstructor @Accessors(chain=true)
public class GameBoardPalette {

    private static final String ANY_UNASSIGNED = "*";

    public static final int DEFAULT_BLANK_RGB = 0xffffff;
    public static final int DEFAULT_BLANK_ANSI = 15;
    @JsonIgnore public int getDefaultBlankColor () { return isRgb() ? DEFAULT_BLANK_RGB : DEFAULT_BLANK_ANSI; }

    public static final int DEFAULT_CURRENT_PLAYER_RGB = 0xff0000;
    public static final int DEFAULT_CURRENT_PLAYER_ANSI = 9;
    @JsonIgnore public int getDefaultCurrentPlayerColor () { return isRgb() ? DEFAULT_CURRENT_PLAYER_RGB : DEFAULT_CURRENT_PLAYER_ANSI; }

    @Getter @Setter private GameBoardPaletteMode mode = GameBoardPaletteMode.rgb;
    @JsonIgnore public boolean isRgb () { return mode == GameBoardPaletteMode.rgb; }
    @JsonIgnore public boolean isAnsi () { return mode == GameBoardPaletteMode.ansi; }
    public void setRgb() { mode = GameBoardPaletteMode.rgb; }
    public void setAnsi() { mode = GameBoardPaletteMode.ansi; }

    @Getter @Setter private String blankColor;
    @Getter @Setter private String currentPlayerId;
    @Getter @Setter private String currentPlayerColor;

    @Getter @Setter private NameAndValue[] playerColors;
    public void addPlayerColor (String player, String color) {
        playerColors = ArrayUtil.append(playerColors, new NameAndValue(player, color));
    }

    private Set<Integer> usedColors = new HashSet<>();

    @JsonIgnore public int getCurrentPlayerColor() { return parseRgb(currentPlayerColor, getDefaultCurrentPlayerColor()); }
    @JsonIgnore public int getBlankColor () { return parseRgb(blankColor, getDefaultBlankColor()); }

    public GameBoardPalette(GameBoardPalette other) { copy(this, other); }

    public static GameBoardPalette defaultPalette(String currentPlayerId) {
        return json(stream2string("palette/default_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public static GameBoardPalette defaultAnsiPalette(String currentPlayerId) {
        return json(stream2string("palette/default_ansi_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public int colorFor(GameTileState tile) {
        int color = _colorFor(tile);
        usedColors.add(color);
        if (tile.hasOwner()) {
            if (NameAndValue.find(playerColors, tile.getOwner()) == null) {
                addPlayerColor(tile.getOwner(), formatColor(color));
            }
        }
        return color;
    }

    private String formatColor(int color) {
        if (isRgb()) {
            final Color c = new Color(color);
            return "0x"
                    +getHexValue((byte) c.getRed())
                    +getHexValue((byte) c.getGreen())
                    +getHexValue((byte) c.getBlue());
        }
        return ""+color;
    }

    public int _colorFor(GameTileState tile) {
        if (tile == null || !tile.hasOwner()) return getBlankColor();
        if (tile.getOwner().equals(currentPlayerId)) {
            return currentPlayerColor == null ? getDefaultCurrentPlayerColor() : parseColor(currentPlayerColor, getDefaultCurrentPlayerColor());
        }
        final String colorString = NameAndValue.find(playerColors, tile.getOwner());
        if (colorString != null) {
            return parseColor(colorString, randomColor());
        }
        return randomColor();
    }

    public int randomColor() {  return isRgb() ? randomRgbColor() : randomAnsiColor(); }

    public int randomRgbColor() {
        int val;
        do {
            val = RandomUtils.nextInt(0x000000, 0xffffff);
        } while (val == getBlankColor() || val == getCurrentPlayerColor() || usedColors.contains(val));
        return val;
    }

    public int randomAnsiColor() {
        int val;
        do {
            val = RandomUtils.nextInt(1, 254);
        }
        while (val == getBlankColor() || val == getCurrentPlayerColor() || usedColors.contains(val));
        return val;
    }

    private int parseColor(String color, int defaultColor) {
        return isRgb() ? parseRgb(color, defaultColor) : parseAnsi(color, defaultColor);
    }

    private int parseAnsi(String color, int defaultColor) {
        try {
            return Integer.parseInt(color);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private int parseRgb(String colorString, int defaultRgb) {
        if (empty(colorString)) return defaultRgb;
        if (colorString.startsWith("0x")) return Integer.parseInt(colorString.substring(2), 16);
        if (colorString.startsWith("#")) return Integer.parseInt(colorString.substring(1), 16);
        return defaultRgb;
    }
}
