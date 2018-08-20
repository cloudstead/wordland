package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.SymbolSetDAO;
import wordland.model.SymbolSet;
import wordland.server.WordlandConfiguration;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import static org.cobbzilla.wizard.resources.ResourceUtil.notFoundEx;
import static wordland.ApiConstants.*;

@Path(SYMBOL_SETS_ENDPOINT)
@Service @Slf4j
public class SymbolSetsResource extends NamedSystemResource<SymbolSet> {

    @Getter @Autowired private SymbolSetDAO dao;
    @Autowired private WordlandConfiguration configuration;

    @Path("/{name}" + EP_POINT_SYSTEMS)
    public PointSystemsResource getPointSystemsResource(@Context HttpContext context,
                                                        @PathParam("name") String name) {
        final SymbolSet symbolSet = dao.findByName(name);
        if (symbolSet == null) throw notFoundEx(name);
        return configuration.subResource(PointSystemsResource.class, symbolSet);
    }

    @Path("/{name}" + EP_DISTRIBUTIONS)
    public SymbolDistributionsResource getSymbolDistributionsResource(@Context HttpContext context,
                                                                      @PathParam("name") String name) {
        final SymbolSet symbolSet = dao.findByName(name);
        if (symbolSet == null) throw notFoundEx(name);
        return configuration.subResource(SymbolDistributionsResource.class, symbolSet);
    }

    @Path("/{name}" + EP_DICTIONARIES)
    public GameDictionariesResource getGameDictionariesResource(@Context HttpContext context,
                                                                @PathParam("name") String name) {
        final SymbolSet symbolSet = dao.findByName(name);
        if (symbolSet == null) throw notFoundEx(name);
        return configuration.subResource(GameDictionariesResource.class, symbolSet);
    }
}
