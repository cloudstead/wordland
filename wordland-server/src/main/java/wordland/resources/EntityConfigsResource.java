package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.entityconfig.ModelSetupListenerBase;
import org.cobbzilla.wizard.resources.AbstractEntityConfigsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import wordland.WordlandApiClient;
import wordland.model.support.AccountSession;
import wordland.server.WordlandConfiguration;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.TreeSet;

import static lombok.AccessLevel.PROTECTED;
import static org.cobbzilla.wizard.model.entityconfig.ModelSetup.setupModel;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static org.cobbzilla.wizard.util.SpringUtil.listResources;
import static wordland.ApiConstants.ENTITY_CONFIGS_ENDPOINT;
import static wordland.ApiConstants.accountPrincipal;

@Path(ENTITY_CONFIGS_ENDPOINT)
@Service @Slf4j
public class EntityConfigsResource extends AbstractEntityConfigsResource {

    @Getter(PROTECTED) @Autowired private WordlandConfiguration configuration;

    @Override protected boolean authorized(HttpContext ctx) {
        final AccountSession session = userPrincipal(ctx);
        return session.hasAccount() && !session.getAccount().isSuspended();
    }

    @GET @Path("/models")
    public Response getModelNames (@Context HttpContext ctx) {

        final AccountSession session = accountPrincipal(ctx);
        if (session == null || !session.hasAccount() || !session.getAccount().isAdmin()) return forbidden();

        final Set<String> names = new TreeSet<>();
        try {
            final Resource[] rs = listResources("models/**/manifest.json");
            for (Resource r : rs) {
                names.add(r.getFile().getParentFile().getName());
            }
        } catch (Exception e) {
            log.error("getModelNames: "+e, e);
            return serverError();
        }
        return ok(names);
    }

    @POST @Path("/models/{name:.+}")
    public Response populateModel (@Context HttpContext ctx,
                                   @PathParam("name") String modelName) {

        final AccountSession session = accountPrincipal(ctx);
        if (session == null || !session.hasAccount() || !session.getAccount().isAdmin()) return forbidden();

        try {
            final WordlandApiClient api = new WordlandApiClient(configuration);
            api.setToken(session.getApiToken());
            return ok(setupModel(api, ENTITY_CONFIGS_ENDPOINT, "models/"+modelName+"/", MODEL_SETUP_LISTENER, "populateModel"));

        } catch (Exception e) {
            log.error("populateModel: "+e.getClass().getSimpleName()+": "+e.getMessage(), e);
            return notFound(modelName);
        }
    }

    public static final ECModelSetupListener MODEL_SETUP_LISTENER = new ECModelSetupListener();
    private static class ECModelSetupListener extends ModelSetupListenerBase {}
}