package wordland.resources;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.PointSystemDAO;
import wordland.model.PointSystem;
import wordland.model.SymbolSet;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@NoArgsConstructor
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class PointSystemsResource {

    private SymbolSet symbolSet;

    @Autowired private PointSystemDAO pointSystemDAO;

    public PointSystemsResource (SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

    @GET @Path("/{name}")
    public Response findPointSystem (@PathParam("name") String name) {
        final PointSystem pointSystem = pointSystemDAO.findBySymbolSetAndName(symbolSet.getName(), name);
        return pointSystem == null ? notFound(name) : ok(pointSystem);
    }

}
