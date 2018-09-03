package wordland.model.game.score;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.game.GamePlayer;
import wordland.model.json.BoardScore;
import wordland.model.support.PlayedTile;

import static org.cobbzilla.util.json.JsonUtil.json;

@NoArgsConstructor @Accessors(chain=true)
public class PlayScoreComponent {

    @Getter @Setter private PlayScoreComponentType type;
    @Getter @Setter private int picas;
    @Getter @Setter private String info;
    @Getter @Setter private boolean absolute;
    @Getter @Setter private String player;
    public boolean hasPlayer() { return player != null; }

    public static PlayScoreComponent symbol(PlayedTile tile, int picas) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.symbol)
                .setPicas(picas)
                .setInfo(json(tile));
    }

    public static PlayScoreComponent word(String word, int picas) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.word)
                .setPicas(picas)
                .setInfo(word);
    }

    public static PlayScoreComponent board(BoardScore b, int picas, boolean absolute, GamePlayer player) {
        return new PlayScoreComponent()
                .setType(PlayScoreComponentType.board)
                .setPicas(picas)
                .setInfo(json(b))
                .setAbsolute(absolute)
                .setPlayer(player == null ? null : player.getId());
    }
}
