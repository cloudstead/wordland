package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECFieldReference;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;
import wordland.model.json.PointSystemSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static wordland.ApiConstants.EP_POINT_SYSTEMS;

@ECType @ECTypeURIs(baseURI=EP_POINT_SYSTEMS)
@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"symbolSet", "name"}, name="point_system_UNIQ_symbol_set_name"))
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
    @ECFieldReference(control="select", refEntity="SymbolSet", refFinder="symbolSets/{symbolSet}", options="uri:symbolSets:name:name")
    @Getter @Setter private String symbolSet;

    @Type(type=PointSystemSettings.JSONB_TYPE) @Column(nullable=false, updatable=false)
    @Getter @Setter private PointSystemSettings settings;

}
