package wordland.model.game.score;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.map.SingletonMap;
import wordland.model.json.BoardScore;
import wordland.model.support.PlayedTile;

import static org.cobbzilla.util.json.JsonUtil.json;

@NoArgsConstructor @Accessors(chain=true)
public class PlayScoreComponent {

    @Getter @Setter private PlayScoreComponentType type;
    @Getter @Setter private int picas;
    @Getter @Setter private JsonNode info;

    @Getter @Setter private Boolean absolute;
    public boolean absolute() { return absolute != null && absolute; }

    @Getter @Setter private String player;
    public boolean hasPlayer() { return player != null; }

    protected static JsonNode toJsonNode(Object thing) { return json(json(thing), JsonNode.class); }

    public static PlayScoreComponent symbol(PlayedTile tile, int picas) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.symbol)
                .setPicas(picas)
                .setInfo(toJsonNode(tile));
    }

    public static PlayScoreComponent word(String word, int picas) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.word)
                .setPicas(picas)
                .setInfo(toJsonNode(new SingletonMap<>("word", word)));
    }

    public static PlayScoreComponent board(BoardScore b, int picas, boolean absolute, String playerId) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.board)
                .setPicas(picas)
                .setInfo(toJsonNode(b))
                .setAbsolute(absolute)
                .setPlayer(playerId);
    }
}
