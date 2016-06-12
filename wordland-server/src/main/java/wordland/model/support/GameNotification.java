package wordland.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @Accessors(chain=true)
public class GameNotification {

    @Getter @Setter private GameNotificationType notification;
    @Getter @Setter private String messageKey;
    @Getter @Setter private String message;
    @Getter @Setter private List<String> params;

    public GameNotification(GameNotificationType notificationType, String messageKey, String message) {
        this.notification = notificationType;
        this.messageKey = messageKey;
        this.message = message;
    }

    public GameNotification addParam (String param) {
        if (params == null) params = new ArrayList<>();
        params.add(param);
        return this;
    }

    public static GameNotification invalidWord(String word) {
        return new GameNotification(GameNotificationType.invalid_word, "err.word.invalid", "sorry, that's not a valid word").addParam(word);
    }

}
