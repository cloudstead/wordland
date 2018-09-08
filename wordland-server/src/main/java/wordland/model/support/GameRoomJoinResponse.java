package wordland.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.game.GamePlayer;
import wordland.model.json.GameRoomSettings;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class GameRoomJoinResponse {

    @Getter @Setter private GamePlayer player;
    @Getter @Setter private String room;
    @Getter @Setter private GameRoomSettings roomSettings;

    public GameRoomJoinResponse(String roomName, GamePlayer player) {
        this.room = roomName;
        this.player = player;
    }

}
