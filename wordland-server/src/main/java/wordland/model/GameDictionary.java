package wordland.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.model.NamedIdentityBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class GameDictionary extends NamedIdentityBase {

    public static final String CLASSPATH_PREFIX = "classpath:";

    @Column(length=1024)
    @JsonIgnore @Getter @Setter private String location;

    // ensure that we never load the words into memory multiple times
    private static Map<String, Set<String>> wordCache = new HashMap<>();

    @JsonIgnore @Transient private volatile Set<String> words = null;
    public GameDictionary setWords (Set<String> words) { return this; } // noop

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

    @JsonIgnore public boolean isWord(String word) {
        if (words == null) {
            synchronized (this) {
                if (words == null) {
                    words = initWords();
                }
            }
        }
        return words.contains(word);
    }

}
