package wordland.model.json;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import wordland.model.game.score.PlayScoreComponent;
import wordland.model.support.PlayedTile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Accessors(chain=true) @Slf4j
public class PointSystemSettings {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_PointSystemSettings";
    public static final JsEngine JS = new StandardJsEngine();

    @Getter @Setter private SymbolScore[] symbolScoring;

    @Getter @Setter private WordScore[] wordScoring;

    @Getter @Setter private BoardScore[] boardScoring;
    public boolean hasBoardScoring() { return !empty(boardScoring); }

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

    public Collection<PlayScoreComponent> scoreBoard(Map<String, Object> ctx) {
        if (boardScoring == null) return null;
        final Collection<PlayScoreComponent> scores = new ArrayList<>();
        for (BoardScore s : boardScoring) {
            try {
                if (JS.evaluateBoolean(s.getCondition(), ctx, false)) {
                    int picas = JS.evaluateInt(s.getPicas(), ctx);
                    scores.add(PlayScoreComponent.board(s, picas, s.isAbsolute()));
                }
            } catch (Exception e) {
                log.warn("scoreBoard("+s.getName()+"): "+e.getClass().getSimpleName()+": "+e);
            }
        }
        return scores;
    }

}
