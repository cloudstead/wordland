package wordland.resources;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.wizard.resources.AbstractCachedNamedSystemSubResource;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.SymbolSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@NoArgsConstructor
public class GameDictionariesResource extends AbstractCachedNamedSystemSubResource<GameDictionariesResource, GameDictionary> {

    private static final Map<String, GameDictionariesResource> resourceCache = new ConcurrentHashMap<>();
    @Override protected Map<String, GameDictionariesResource> getCacheMap() { return resourceCache; }

    @Getter @Autowired private GameDictionaryDAO dao;

    private SymbolSet symbolSet;
    public GameDictionariesResource(SymbolSet symbolSet) { this.symbolSet = symbolSet; }

}
