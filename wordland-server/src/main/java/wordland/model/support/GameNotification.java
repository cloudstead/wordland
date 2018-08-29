package wordland.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.collection.NameAndValue;

import static org.cobbzilla.util.json.JsonUtil.json;

@NoArgsConstructor @Accessors(chain=true)
public class GameNotification {

    public static final String PARAM_WORD = "word";
    public static final String PARAM_TILE = "tile";

    @Getter @Setter private GameNotificationType notification;
    @Getter @Setter private String messageKey;
    @Getter @Setter private String message;
    @Getter @Setter private NameAndValue[] params;

    public GameNotification(GameNotificationType notificationType, String messageKey, String message) {
        this.notification = notificationType;
        this.messageKey = messageKey;
        this.message = message;
    }

    public GameNotification addParam (String name, String value) {
        if (params == null) {
            params = new NameAndValue[1];
            params[0] = new NameAndValue(name, value);
        } else {
            params = ArrayUtil.append(params, new NameAndValue(name, value));
        }
        return this;
    }

    public String param(String name) { return NameAndValue.find(params, name); }

    public static GameNotification invalidWord(String word) {
        return new GameNotification(GameNotificationType.invalid_word, "err.word.invalid", "sorry, that's not a valid word")
                .addParam(PARAM_WORD, word);
    }

    public static GameNotification sparseWord(String word, int max, PlayedTile farTile) {
        return new GameNotification(GameNotificationType.sparse_word, "err.word.sparse", "letters must be no farther than "+max+"letters apart in all directions")
                .addParam(PARAM_WORD, word)
                .addParam(PARAM_TILE, json(farTile));
    }

}
