package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.javascript.StandardJsEngine;
import wordland.model.game.GamePlayer;
import wordland.model.game.score.PlayScoreComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static wordland.model.game.GameState.CTX_PLAYER;
import static wordland.model.game.GameState.CTX_PLAYERS;

@Slf4j
public class BoardScore {

    public static final JsEngine JS = new StandardJsEngine();

    @Getter @Setter private String name;
    @Getter @Setter private String condition;
    @Getter @Setter private String picas;
    @Getter @Setter private boolean absolute;
    @Getter @Setter private boolean allPlayers;

    public Collection<PlayScoreComponent> score(Map<String, Object> ctx) {
        final List<PlayScoreComponent> components = new ArrayList<>();
        try {
            if (allPlayers) {
                final Collection<GamePlayer> players = (Collection<GamePlayer>) ctx.get(CTX_PLAYERS);
                for (GamePlayer player : players) {
                    ctx.put(CTX_PLAYER, player);
                    components.add(evalForPlayer(player, ctx));
                }
            } else {
                components.add(evalForPlayer(null, ctx));
            }
        } catch (Exception e) {
            return die("score("+getName()+"): "+e.getClass().getSimpleName()+": "+e);
        }
        return components;
    }

    protected PlayScoreComponent evalForPlayer(GamePlayer player, Map<String, Object> ctx) {
        if (JS.evaluateBoolean(getCondition(), ctx, false)) {
            int picas = JS.evaluateInt(getPicas(), ctx);
            return PlayScoreComponent.board(this, picas, isAbsolute(), player);
        }
        return null;
    }
}
