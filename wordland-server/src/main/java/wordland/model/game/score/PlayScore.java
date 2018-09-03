package wordland.model.game.score;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayScore {

    @Getter @Setter private List<PlayScoreComponent> scores;
    @Getter @Setter private boolean absolute;

    public int getTotal () {
        int picas = 0;
        if (scores != null) {
            for (PlayScoreComponent c : scores) {
                if (c.isAbsolute()) return c.getPicas();
                picas += c.getPicas();
            }
        }
        return picas;
    }
    public void setTotal (int total) {} // noop

    public void addScore(PlayScoreComponent c) {
        if (c == null) return;
        if (scores == null) scores = new ArrayList<>();
        scores.add(c);
    }

    public void addScores(Collection<PlayScoreComponent> components) {
        if (components == null) return;
        if (scores == null) scores = new ArrayList<>();
        for (PlayScoreComponent c : components) {
            if (c.isAbsolute()) setAbsolute(true);
            scores.add(c);
        }
    }

}
