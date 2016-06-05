package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;
import wordland.model.json.SymbolDistributionSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.Valid;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class SymbolDistribution extends NamedIdentityBase {

    public SymbolDistribution (SymbolDistribution other) {
        super(other.getName());
        setSymbolSet(other.getSymbolSet());
        setSettings(other.getSettings());
    }

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSettings(((SymbolDistribution) other).getSettings());
    }

    @HasValue(message="err.symbolSet.empty")
    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String symbolSet;

    @Type(type=SymbolDistributionSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Valid @Getter @Setter private SymbolDistributionSettings settings;

}
