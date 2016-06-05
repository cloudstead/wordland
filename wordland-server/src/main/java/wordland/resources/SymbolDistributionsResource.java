package wordland.resources;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.wizard.resources.AbstractCachedNamedSystemSubResource;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.SymbolDistributionDAO;
import wordland.model.SymbolDistribution;
import wordland.model.SymbolSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class SymbolDistributionsResource
        extends AbstractCachedNamedSystemSubResource<SymbolDistributionsResource, SymbolDistribution> {

    private static final Map<String, SymbolDistributionsResource> resourceCache = new ConcurrentHashMap<>();
    @Override protected Map<String, SymbolDistributionsResource> getCacheMap() { return resourceCache; }

    @Getter @Autowired private SymbolDistributionDAO dao;

    private SymbolSet symbolSet;
    public SymbolDistributionsResource(SymbolSet symbolSet) { this.symbolSet = symbolSet; }

}