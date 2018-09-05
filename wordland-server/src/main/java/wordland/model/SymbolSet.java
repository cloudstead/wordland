package wordland.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.emory.mathcs.backport.java.util.Arrays;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.model.entityconfig.NamedParentEntity;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECType;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeChild;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeChildren;
import org.cobbzilla.wizard.model.entityconfig.annotations.ECTypeURIs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;

import static wordland.ApiConstants.SYMBOL_SETS_ENDPOINT;

@ECType(root=true)
@ECTypeURIs(baseURI=SYMBOL_SETS_ENDPOINT)
@ECTypeChildren(uriPrefix=SYMBOL_SETS_ENDPOINT + "/{SymbolSet.name}",
        value={@ECTypeChild(type=PointSystem.class,        backref="symbolSet"),
               @ECTypeChild(type=GameDictionary.class,     backref="symbolSet"),
               @ECTypeChild(type=SymbolDistribution.class, backref="symbolSet")})
@Entity @NoArgsConstructor @Accessors(chain=true)
public class SymbolSet extends NamedParentEntity {

    public SymbolSet (SymbolSet other) { super(other.getName()); update(other); }

    @Override public NamedIdentityBase update(NamedIdentityBase other) {
        return setSyms(((SymbolSet) other).getSyms());
    }

    @Column(length=100_000, nullable=false, updatable=false)
    @JsonIgnore @Getter @Setter private String syms;

    @Transient public List<String> getSymbols () { return syms == null ? null : Arrays.asList(syms.split("[-\\[, _\t\n]+")); }
    public SymbolSet setSymbols (List<String> symbols) { syms = StringUtil.toString(symbols, ", "); return this; }

}
