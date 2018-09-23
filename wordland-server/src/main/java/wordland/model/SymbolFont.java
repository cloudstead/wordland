package wordland.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;

import javax.persistence.Column;
import javax.persistence.Entity;

import static wordland.ApiConstants.SYMBOL_FONTS_ENDPOINT;

@ECType(root=true) @ECTypeURIs(baseURI= SYMBOL_FONTS_ENDPOINT)
@Entity @NoArgsConstructor @Accessors(chain=true)
public class SymbolFont extends NamedIdentityBase {

    @SuppressWarnings("unused") // called by AbstractDAO.newEntity
    public SymbolFont (SymbolFont other) {
        update((Identifiable) other);
        setName(other.getName());
    }

    @Override public NamedIdentityBase update(NamedIdentityBase other) { update((Identifiable) other); return this; }

    @Column(columnDefinition="TEXT")
    @Getter @Setter private String base64ttf;

}
