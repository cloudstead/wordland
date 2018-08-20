package wordland.resources;

import lombok.NoArgsConstructor;
import wordland.model.SymbolSet;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@NoArgsConstructor
public class GameDictionariesResource {

    private SymbolSet symbolSet;

    public GameDictionariesResource(SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

}
