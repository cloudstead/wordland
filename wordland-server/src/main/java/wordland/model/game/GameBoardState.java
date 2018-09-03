package wordland.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.NameAndValue;
import wordland.model.support.AttemptedTile;
import wordland.model.support.PlayedTile;
import wordland.model.support.TextGridResponse;

import java.util.*;

import static wordland.model.game.TileFunctions.MATCH_CLAIMED;
import static wordland.model.game.TileFunctions.countUnclaimed;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class GameBoardState {

    @Getter @Setter private RoomState roomState;
    @Getter @Setter private long version;
    @Getter @Setter private int x1;
    @Getter @Setter private int x2;
    @Getter @Setter private int y1;
    @Getter @Setter private int y2;
    @Getter @Setter private GameTileState[][] tiles;

    public GameBoardState(long version, int x1, int x2, int y1, int y2, RoomState roomState) {
        this(roomState, version, x1, x2, y1, y2, null);
    }

    public String grid () { return TileGridFunctions.grid(tiles); }
    public String grid (GameBoardPalette palette) { return TileGridFunctions.grid(tiles, palette); }
    public TextGridResponse grid (GameBoardPalette palette, AttemptedTile[] attempt) { return TileGridFunctions.grid(tiles, palette, attempt); }

    @SuppressWarnings("unused") // used in JSON tests. see models/infinity/tests/play_infinity.json
    public boolean claimedWord(String word) {
        for (GameTileState[] row : tiles) {
            for (GameTileState tile : row) {
                if (!tile.hasOwner()) continue;
                word = word.replaceFirst(tile.getSymbol(), "");
                if (word.length() == 0) return true;
            }
        }
        return false;
    }

    public int unclaimed () { return countUnclaimed(tiles); }
    public boolean allClaimed() { return unclaimed() == 0; }
    @JsonIgnore public boolean getAllClaimed() { return allClaimed(); }

    @JsonIgnore @Getter(lazy=true) private final List<NameAndValue> playersByCount = initPlayersByCount();
    private List<NameAndValue> initPlayersByCount() {
        final Map<String, Integer> counts = TileFunctions.forEachTile(tiles, new TileMapReduce<String, Map<String, Integer>>()
                .setMatch(MATCH_CLAIMED)
                .setReducer((tiles, x, y) -> tiles[x][y].getOwner())
                .setAccumulator(new GameTileAccumulator<String, Map<String, Integer>>() {
                    @Getter final Map<String, Integer> total = new HashMap<>();
                    @Override public void add(GameTileState[][] tiles, int x, int y, String owner) {
                        final Integer count = total.computeIfAbsent(owner, k -> 0);
                        total.put(owner, count+1);
                    }
                }));

        final List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        Collections.sort(entries, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        final List<NameAndValue> playersByCount = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            playersByCount.add(new NameAndValue(entry.getKey(), ""+entry.getValue()));
        }
        return playersByCount;
    }

    @SuppressWarnings("unused") // used in gameRoom.json for metapress room, in winConditions
    @JsonIgnore public Collection<String> getTopPlayersByCount () {
        final List<NameAndValue> counts = getPlayersByCount();
        final List<String> tops = new ArrayList<>();
        Integer topCount = null;
        for (NameAndValue c : counts) {
            if (topCount == null) {
                topCount = Integer.valueOf(c.getValue());
                tops.add(c.getName());
            } else if (topCount.equals(Integer.valueOf(c.getValue()))) {
                tops.add(c.getName());
            } else {
                break;
            }
        }
        return tops;
    }

    public void setOwner(String owner, Collection<PlayedTile> tiles) {
        final GameTileState[][] board = getTiles();
        for (PlayedTile t : tiles) {
            board[t.getX()][t.getY()].setOwner(owner);
        }
    }
}
