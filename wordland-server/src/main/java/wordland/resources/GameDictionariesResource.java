package wordland.resources;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.SymbolSet;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@NoArgsConstructor
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class GameDictionariesResource {

    private SymbolSet symbolSet;

    @Autowired private GameDictionaryDAO dictionaryDAO;

    public GameDictionariesResource(SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

    @GET @Path("/{name}")
    public Response findGameDictionary (@PathParam("name") String name) {
        final GameDictionary dictionary = dictionaryDAO.findBySymbolSetAndName(symbolSet.getName(), name);
        return dictionary == null ? notFound(name) : ok(dictionary);
    }

}
