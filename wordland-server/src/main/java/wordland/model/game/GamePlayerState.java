package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class GamePlayerState {

    @Getter @Setter private Integer version;

}
