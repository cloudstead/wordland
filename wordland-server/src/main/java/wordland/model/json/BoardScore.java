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
import static wordland.model.game.GameState.CTX_SCOREBOARD;

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
                final Object savedPlayer = ctx.get(CTX_PLAYER);
                try {
                    final Map<String, String> scoreboard = (Map<String, String>) ctx.get(CTX_SCOREBOARD);
                    final Map<String, GamePlayer> players = (Map<String, GamePlayer>) ctx.get(CTX_PLAYERS);
                    for (String playerId : scoreboard.keySet()) {

                        final GamePlayer player = players.containsKey(playerId)
                                // try to use real GamePlayer for "player" variable
                                ? players.get(playerId)
                                // but if the player left the game, this will be null, so use a dummy GamePlayer with proper id
                                : new GamePlayer().setId(playerId).setName(playerId);
                        ctx.put(CTX_PLAYER, player);

                        final PlayScoreComponent component = evalForPlayer(playerId, ctx);
                        if (component != null) components.add(component);
                    }
                } finally {
                    // restore
                    ctx.put(CTX_PLAYER, savedPlayer);
                }
            } else {
                final PlayScoreComponent component = evalForPlayer(null, ctx);
                if (component != null) components.add(component);
            }
        } catch (Exception e) {
            return die("score("+getName()+"): "+e.getClass().getSimpleName()+": "+e);
        }
        return components;
    }

    protected PlayScoreComponent evalForPlayer(String playerId, Map<String, Object> ctx) {
        if (JS.evaluateBoolean(getCondition(), ctx, false)) {
            int picas = JS.evaluateInt(getPicas(), ctx);
            return PlayScoreComponent.board(this, picas, isAbsolute(), playerId);
        }
        return null;
    }
}
