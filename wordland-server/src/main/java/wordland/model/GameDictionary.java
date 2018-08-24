package wordland.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECFieldReference;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;
import org.cobbzilla.wizard.validation.HasValue;
import wordland.model.game.GameTileStateExtended;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;

import javax.persistence.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.cobbzilla.util.daemon.ZillaRuntime.CLASSPATH_PREFIX;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static wordland.ApiConstants.EP_DICTIONARIES;

@ECType @ECTypeURIs(baseURI=EP_DICTIONARIES)
@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"symbolSet", "name"}, name="game_dictionary_UNIQ_symbol_set_name"))
public class GameDictionary extends NamedIdentityBase {

    @HasValue(message="err.symbolSet.empty")
    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @ECFieldReference(control="select", refEntity="SymbolSet", refFinder="symbolSets/{symbolSet}", options="uri:symbolSets:name:name")
    @Getter @Setter private String symbolSet;

    @Column(length=1024)
    @JsonIgnore @Getter @Setter private String location;

    // ensure that we never load the words into memory multiple times
    private static Map<String, Set<String>> wordCache = new HashMap<>();

    @JsonIgnore @Transient private AutoRefreshingReference<Set<String>> words = new AutoRefreshingReference<Set<String>>() {
        @Override public Set<String> refresh() { wordCache.clear(); return initWords(); }
        @Override public long getTimeout() { return DAYS.toMillis(1); }
    };
    public GameDictionary setWords (Set<String> words) { return this; } // noop

    private Set<String> words() { return words.get(); }

    private Set<String> initWords() {
        Set<String> set = wordCache.get(getName());
        if (set == null) {
            if (location == null) location = CLASSPATH_PREFIX + "dict/en_US.txt";
            if (location.startsWith(CLASSPATH_PREFIX)) {
                try {
                    @Cleanup final InputStream in = StreamUtil.loadResourceAsStream(location.substring(CLASSPATH_PREFIX.length()));
                    final BufferedReader r = new BufferedReader(new InputStreamReader(in));
                    set = new HashSet<>(250_000);
                    String line;
                    while ((line = r.readLine()) != null) {
                        set.add(line.trim().toLowerCase());
                    }
                    wordCache.put(getName(), set);

                } catch (Exception e) {
                    return die("initWords: error initializing dictionary (" + getName() + "): " + e, e);
                }
            } else {
                return die("initWords: error initializing dictionary (" + getName() + "): invalid location: " + getLocation());
            }
        }
        return set;
    }

    @JsonIgnore @Transient public boolean isWord(String word) {
        return words().contains(word);
    }

    @JsonIgnore @Transient @Getter(lazy=true) private final List<String> shortestFirst = initShortestFirst();
    private List<String> initShortestFirst() {
        final SortedSet<String> shorties = new TreeSet<>(comparingInt(String::length).thenComparing(s -> s));
        final Set<String> allWords = words();
        for (String word : allWords) {
            if (word.length() > 1 && word.length() < 6) shorties.add(word);
        }
        return new ArrayList<>(shorties);
    }


    public GameRuntimeEvent findWord(List<GameTileStateExtended> tiles) {
        final List<Character> letters = tiles.stream().map((t) -> t.getSymbol().charAt(0)).collect(Collectors.toList());
        for (String word : getShortestFirst()) {
            char[] charsInWord = word.toCharArray();
            final List<Character> test = new ArrayList<>(letters);
            boolean ok = true;
            for (char c : charsInWord) {
                if (!test.remove((Character) c)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return new GameRuntimeEvent().setWord(word).setTiles(pickTiles(tiles, word));
            }
        }
        return null;
    }

    private PlayedTile[] pickTiles(List<GameTileStateExtended> tiles, String word) {
        final List<PlayedTile> playedTiles = new ArrayList<>();
        final Set<GameTileStateExtended> picked = new HashSet<>();
        for (int i=0; i<word.length(); i++) {
            GameTileStateExtended matchingTile = null;
            for (GameTileStateExtended tile : tiles) {
                if (picked.contains(tile)) continue;
                if (tile.getSymbol().charAt(0) == word.charAt(i)) {
                    matchingTile = tile;
                    break;
                }
            }
            if (matchingTile != null) {
                playedTiles.add(new PlayedTile(matchingTile.getX(), matchingTile.getY(), matchingTile.getSymbol()));
                picked.add(matchingTile);
            } else {
                return die("pickTiles: cannot find tile with symbol "+word.charAt(i));
            }
        }
        return playedTiles.toArray(new PlayedTile[playedTiles.size()]);
    }
}
