package wordland.model.json;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static wordland.ApiConstants.STANDARD;

@NoArgsConstructor @Accessors(chain=true)
public class GameRoomSettingsValues {

    @Getter @Setter private String symbolSet = STANDARD;
    @Getter @Setter private String defaultDistribution = STANDARD;
    @Getter @Setter private String pointSystem = STANDARD;
    @Getter @Setter private String board = STANDARD;
    @Getter @Setter private String dictionary = STANDARD;

    @Getter @Setter private boolean teamPlay = false;
    @Getter @Setter private int maxPlayers = 100;

}
