package wordland.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

@NoArgsConstructor @Accessors(chain=true)
public class ScoreboardEntry implements Comparable<ScoreboardEntry> {

    @Getter @Setter private String id;

    @Getter private String name;
    public ScoreboardEntry setName (String val) { this.name = StringUtil.ellipsis(val, 16); return this; }

    @Getter @Setter private int score;

    @Override public int compareTo(ScoreboardEntry e) { return Integer.compare(getScore(), e.getScore()); }

}
