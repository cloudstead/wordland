package wordland.resources;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.wizard.resources.AbstractCachedNamedSystemSubResource;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.PointSystemDAO;
import wordland.model.PointSystem;
import wordland.model.SymbolSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class PointSystemsResource
        extends AbstractCachedNamedSystemSubResource<PointSystemsResource, PointSystem> {

    private static final Map<String, PointSystemsResource> resourceCache = new ConcurrentHashMap<>();
    @Override protected Map<String, PointSystemsResource> getCacheMap() { return resourceCache; }

    @Getter @Autowired private PointSystemDAO dao;

    private SymbolSet symbolSet;
    public PointSystemsResource (SymbolSet symbolSet) { this.symbolSet = symbolSet; }

}
