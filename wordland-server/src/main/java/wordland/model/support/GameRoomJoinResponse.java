package wordland.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.game.GamePlayer;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class GameRoomJoinResponse {

    @Getter @Setter private String room;
    @Getter @Setter private GamePlayer player;

}
