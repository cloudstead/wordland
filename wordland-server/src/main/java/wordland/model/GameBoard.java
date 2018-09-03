package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;
import org.hibernate.annotations.Type;
import wordland.model.json.GameBoardSettings;

import javax.persistence.Column;
import javax.persistence.Entity;

import static wordland.ApiConstants.GAME_BOARDS_ENDPOINT;

@ECType(root=true)
@ECTypeURIs(baseURI=GAME_BOARDS_ENDPOINT)
@Entity @NoArgsConstructor @Accessors(chain=true)
public class GameBoard extends NamedIdentityBase {

    @Type(type=GameBoardSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private GameBoardSettings settings;

    public boolean infinite() { return settings.infinite(); }

}
