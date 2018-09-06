package wordland.model.game.score;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import wordland.model.game.GamePlayer;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;

@Accessors(chain=true) @Slf4j
public class PlayScore {

    @Getter @Setter private GamePlayer player;
    @Getter @Setter private List<PlayScoreComponent> scores;

    @Getter @Setter private Boolean absolute;
    public boolean absolute() { return absolute != null && absolute; }

    public int getTotal () { return getTotals().getOrDefault(player.getId(), 0); }
    public void setTotal (Integer v) {} // noop

    @Getter(lazy=true) private final Map<String, Integer> totals = initTotals();
    private Map<String, Integer> initTotals () {

        final Map<String, Integer> totals = new HashMap<>();
        if (empty(scores)) return totals; // sanity check

        Map<String, Integer> absTotals = null;

        for (PlayScoreComponent scoreComponent : scores) {
            final String player = scoreComponent.getPlayer();
            final int picas = scoreComponent.getPicas();

            if (scoreComponent.absolute()) {
                if (absTotals == null) {
                    absTotals = new HashMap<>();

                } else if (absTotals.containsKey(player)) {
                    // already set absolute
                    log.warn("getTotals: multiple absolute pica scores found:\n"+json(scores));
                    continue;
                }
                absTotals.put(player, picas);
                totals.put(player, picas);

            } else {
                final int currentTotal = totals.computeIfAbsent(player, k -> 0);
                totals.put(player, currentTotal+picas);
            }
        }
        return totals;
    }

    public void setTotals (Map<String, Integer> map) {} // noop

    public int getTotal (String playerId) { return getTotals().getOrDefault(playerId, 0); }

    public void addScore(PlayScoreComponent c) {
        if (c == null) return;
        if (scores == null) scores = new ArrayList<>();
        if (c.absolute()) setAbsolute(true);
        if (!c.hasPlayer()) c.setPlayer(getPlayer().getId());
        scores.add(c);
    }

    public void addScores(Collection<PlayScoreComponent> components) {
        if (components == null) return;
        for (PlayScoreComponent c : components) addScore(c);
    }

}
