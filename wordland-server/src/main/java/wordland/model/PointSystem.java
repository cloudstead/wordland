package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;
import wordland.model.json.PointSystemSettings;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class PointSystem extends NamedIdentityBase {

    public PointSystem (PointSystem other) {
        super(other.getName());
        setSymbolSet(other.getSymbolSet());
        setSettings(other.getSettings());
    }

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSettings(((PointSystem) other).getSettings());
    }

    @HasValue(message="err.symbolSet.empty")
    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String symbolSet;

    @Type(type=PointSystemSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private PointSystemSettings settings;

}
