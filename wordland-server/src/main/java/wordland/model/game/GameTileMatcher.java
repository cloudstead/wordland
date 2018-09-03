package wordland.model.game;

import static org.cobbzilla.util.string.StringUtil.isVowel;

public interface GameTileMatcher {

    GameTileMatcher MATCH_CLAIMED = (tiles, x, y) -> tiles[x][y].hasOwner();
    GameTileMatcher MATCH_UNCLAIMED = (tiles, x, y) -> tiles[x][y].unclaimed();
    GameTileMatcher MATCH_ANY_CLAIMED = (tiles, x, y) -> TileFunctions.countClaimed(tiles) > 0;
    GameTileMatcher MATCH_ANY_UNCLAIMED = (tiles, x, y) -> TileFunctions.countUnclaimed(tiles) > 0;
    GameTileMatcher MATCH_ALL_TILES = (tiles, x, y) -> true;

    boolean matches(GameTileState[][] tiles, int x, int y);

    GameTileMatcher MATCH_VOWEL = (tiles, x, y) -> isVowel(tiles[x][y].getSymbol());

}
