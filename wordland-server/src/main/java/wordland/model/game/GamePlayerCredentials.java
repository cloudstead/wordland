package wordland.model.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class GamePlayerCredentials {

    @Getter @Setter private String name;
    @Getter @Setter private String team;
    @Getter @Setter private String id;
    @Getter @Setter private String apiKey;

}
