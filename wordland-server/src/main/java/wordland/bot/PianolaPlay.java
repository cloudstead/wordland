package wordland.bot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.game.GamePlayer;
import wordland.model.support.GameRuntimeEvent;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class PianolaPlay {

    @Getter @Setter private GamePlayer player;
    @Getter @Setter private String roomName;
    @Getter @Setter private GameRuntimeEvent event;

    public GameRuntimeEvent initEvent() {
        final GameRuntimeEvent e = getEvent();
        if (e != null && player != null) {
            e.setApiToken(player.getApiToken());
            e.setId(player.getId());
            e.setClientId(player.getId());
            e.setRoom(roomName);
        }
        return e;
    }
}
