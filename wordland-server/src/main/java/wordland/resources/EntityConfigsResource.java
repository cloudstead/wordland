package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.AccessLevel;
import lombok.Getter;
import org.cobbzilla.wizard.resources.AbstractEntityConfigsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.model.Account;
import wordland.server.WordlandConfiguration;

import javax.ws.rs.Path;

import static org.cobbzilla.wizard.resources.ResourceUtil.userPrincipal;
import static wordland.ApiConstants.ENTITY_CONFIGS_ENDPOINT;

@Path(ENTITY_CONFIGS_ENDPOINT)
@Service
public class EntityConfigsResource extends AbstractEntityConfigsResource {

    @Getter(AccessLevel.PROTECTED) @Autowired private WordlandConfiguration configuration;

    @Override protected boolean authorized(HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return !account.isSuspended();
    }

}