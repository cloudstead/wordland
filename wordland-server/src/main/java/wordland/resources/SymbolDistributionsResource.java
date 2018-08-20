package wordland.resources;

import lombok.NoArgsConstructor;
import wordland.model.SymbolSet;

@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class SymbolDistributionsResource {

    private SymbolSet symbolSet;

    public SymbolDistributionsResource(SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

}