package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;

import java.util.Collection;
import java.util.HashSet;

import static org.cobbzilla.util.graphics.ColorUtil.*;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

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

    @Getter(lazy=true) private final Collection<Integer> usedColors = initUsedColors();
    private HashSet<Integer> initUsedColors() {
        final HashSet<Integer> colors = new HashSet<>();
        colors.add(getBlankColor());
        colors.add(getCurrentPlayerColor());
        return colors;
    }

    @JsonIgnore public int getCurrentPlayerColor() { return parseRgb(currentPlayerColor, getDefaultCurrentPlayerColor()); }
    @JsonIgnore public int getBlankColor () { return parseRgb(blankColor, getDefaultBlankColor()); }

    public GameBoardPalette(GameBoardPalette other) { copy(this, other); }

    public static GameBoardPalette defaultPalette(String currentPlayerId) {
        return json(stream2string("palette/default_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public int colorFor(GameTileState tile) {
        int color = _colorFor(tile);
        getUsedColors().add(color);
        if (tile.hasOwner()) {
            if (NameAndValue.find(playerColors, tile.getOwner()) == null) {
                addPlayerColor(tile.getOwner(), formatColor(color));
            }
        }
        return color;
    }

    private String formatColor(int color) { return isRgb() ? rgb_hex(color) : "" + color; }

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

    public int randomColor() {  return isRgb() ? randomRgbColor(getUsedColors()) : randomAnsiColor(getUsedColors()); }

    private int parseColor(String color, int defaultColor) {
        return isRgb() ? parseRgb(color, defaultColor) : parseAnsi(color, defaultColor);
    }

}
