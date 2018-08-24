package wordland.model.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import wordland.model.support.GameNotification;

@AllArgsConstructor
public class GameNotificationException extends RuntimeException {

    @Getter private final GameNotification gameNotification;

}
