package wordland.resources;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import wordland.dao.SymbolDistributionDAO;
import wordland.model.SymbolDistribution;
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
public class SymbolDistributionsResource {

    private SymbolSet symbolSet;

    @Autowired private SymbolDistributionDAO distributionDAO;

    public SymbolDistributionsResource(SymbolSet symbolSet) {
        this.symbolSet = symbolSet;
    }

    @GET @Path("/{name}")
    public Response findDistribution (@PathParam("name") String name) {
        final SymbolDistribution distribution = distributionDAO.findBySymbolSetAndName(symbolSet.getName(), name);
        return distribution == null ? notFound(name) : ok(distribution);
    }

}