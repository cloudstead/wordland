package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.graphics.ColorMode;

import java.util.HashSet;
import java.util.Set;

import static org.cobbzilla.util.graphics.ColorUtil.*;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;

@NoArgsConstructor @Accessors(chain=true)
public class GameBoardPalette {

    private static final String ANY_UNASSIGNED = "*";

    public static final int DEFAULT_BLANK_COLOR = 0xffffff;
    public static final int DEFAULT_CURRENT_PLAYER_COLOR = 0xff0000;

    @Getter @Setter private ColorMode mode = ColorMode.rgb;
    @JsonIgnore public boolean isRgb () { return mode == ColorMode.rgb; }
    @JsonIgnore public boolean isAnsi () { return mode == ColorMode.ansi; }
    public void setRgb() { mode = ColorMode.rgb; }
    public void setAnsi() { mode = ColorMode.ansi; }

    @Getter @Setter private String blankColor;
    @Getter @Setter private String currentPlayerId;
    @Getter @Setter private String currentPlayerColor;

    @Getter @Setter private NameAndValue[] playerColors;
    public void addPlayerColor (String player, String color) {
        playerColors = ArrayUtil.append(playerColors, new NameAndValue(player, color));
    }

    @JsonIgnore private Set<Integer> usedColors = null;

    public void init () {
        usedColors = new HashSet<>();
        usedColors.add(getBlankRgb());
        usedColors.add(getCurrentPlayerRgb());
        for (NameAndValue playerColor : playerColors) {
            playerColor.setValue(rgb2hex(parseRgb(playerColor.getValue())));
        }
        blankColor = rgb2hex(parseRgb(blankColor, DEFAULT_BLANK_COLOR));
        currentPlayerColor = rgb2hex(parseRgb(currentPlayerColor, DEFAULT_CURRENT_PLAYER_COLOR));
    }

    @JsonIgnore public int getCurrentPlayerRgb() { return parseRgb(currentPlayerColor); }
    @JsonIgnore public int getBlankRgb () { return parseRgb(blankColor); }

    public static GameBoardPalette defaultPalette(String currentPlayerId) {
        return json(stream2string("palette/default_palette.json"), GameBoardPalette.class)
                .setCurrentPlayerId(currentPlayerId);
    }

    public int colorFor(GameTileState tile) {
        int color = _colorFor(tile);
        usedColors.add(color);
        if (tile.hasOwner()) {
            if (NameAndValue.find(playerColors, tile.getOwner()) == null) {
                addPlayerColor(tile.getOwner(), rgb2hex(color));
            }
        }
        return color;
    }

    public int _colorFor(GameTileState tile) {
        if (tile == null || !tile.hasOwner()) return getBlankRgb();
        if (tile.getOwner().equals(currentPlayerId)) return getCurrentPlayerRgb();
        final String colorString = NameAndValue.find(playerColors, tile.getOwner());
        if (colorString != null) return parseRgb(colorString);
        for (NameAndValue c : playerColors) {
            if (c.getName().equals(ANY_UNASSIGNED)) {
                c.setName(tile.getOwner());
                return parseRgb(c.getValue());
            }
        }
        return randomColor(usedColors, mode);
    }

}
