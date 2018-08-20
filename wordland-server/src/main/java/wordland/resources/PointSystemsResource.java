package wordland.resources;

import lombok.NoArgsConstructor;
import wordland.model.SymbolSet;

@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class PointSystemsResource {

    private SymbolSet symbolSet;

    public PointSystemsResource (SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

}
