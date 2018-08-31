package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameStateStorageService;
import wordland.model.game.score.PlayScoreComponent;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Accessors(chain=true) @Slf4j
public class PointSystemSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_PointSystemSettings";
    public static final JsEngine JS = new StandardJsEngine();

    @Getter @Setter private SymbolScore[] symbolScoring;
    @Getter @Setter private WordScore[] wordScoring;
    @Getter @Setter private BoardScore[] boardScoring;

    public PlayScoreComponent scoreLetter(PlayedTile tile) {
        if (symbolScoring != null) {
            for (SymbolScore s : symbolScoring) {
                if (s.getSymbol().equalsIgnoreCase(tile.getSymbol())) {
                    return PlayScoreComponent.symbol(tile, s.getPicas());
                }
            }
        }
        return null;
    }

    public PlayScoreComponent scoreWord(String word) {
        if (wordScoring != null) {
            for (WordScore s : wordScoring) {
                if (s.getLength() >= word.length()) {
                    return PlayScoreComponent.word(word, s.getPicas());
                }
            }
        }
        return null;
    }

    public Collection<PlayScoreComponent> scoreBoard(GameStateStorageService stateStorage, GamePlayer player, String word, PlayedTile[] tiles) {
        if (boardScoring == null) return null;
        final Collection<PlayScoreComponent> scores = new ArrayList<>();
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("state", stateStorage);
        ctx.put("player", player);
        ctx.put("word", word);
        ctx.put("tiles", tiles);
        for (BoardScore s : boardScoring) {
            try {
                if (JS.evaluateBoolean(s.getCondition(), ctx, false)) {
                    int picas = JS.evaluateInt(s.getPicas(), ctx);
                    scores.add(PlayScoreComponent.board(s, picas));
                }
            } catch (Exception e) {
                log.warn("scoreBoard("+s.getName()+"): "+e.getClass().getSimpleName()+": "+e);
            }
        }
        return scores;
    }
}
