package wordland.model.game;

import static org.cobbzilla.util.string.StringUtil.isVowel;

public interface GameTileMatcher {

    boolean matches(GameTileState[][] tiles, int x, int y);

    GameTileMatcher MATCH_VOWEL = (tiles, x, y) -> isVowel(tiles[x][y].getSymbol());

}
