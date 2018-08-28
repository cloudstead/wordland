package wordland.auth;

import com.sun.jersey.spi.container.ContainerRequest;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import wordland.model.support.AccountSession;
import wordland.server.WordlandConfiguration;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.StringPrefixTransformer;
import org.cobbzilla.wizard.filters.auth.AuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static wordland.ApiConstants.*;

@Provider @Service
public class WordlandAuthFilter extends AuthFilter<AccountSession> {

    @Override public String getAuthTokenHeader() { return API_TOKEN; }
    @Getter private final Set<String> skipAuthPaths = Collections.emptySet();

    @Autowired private WordlandConfiguration configuration;
    @Autowired @Getter private WordlandAuthProvider authProvider;

    private Set<String> prefixSet(String[] paths) {
        final StringPrefixTransformer transformer = new StringPrefixTransformer(configuration.getHttp().getBaseUri());
        final List<String> prefixes = Arrays.asList(paths);
        return new HashSet<>(CollectionUtils.collect(prefixes, transformer));
    }

    @Getter(lazy=true) private final Set<String> skipAuthPrefixes = initSkipAuthPrefixes();
    public Set<String> initSkipAuthPrefixes() {
        return prefixSet(new String[] {
                AUTH_ENDPOINT
        });
    }

    @Getter(lazy=true) private final Set<String> adminRequiredPrefixes = initAdminRequiredPrefixes();
    private Set<String> initAdminRequiredPrefixes() { return prefixSet(new String[] {"/admin"}); }

    @Override protected boolean isPermitted(AccountSession principal, ContainerRequest request) {
        if (startsWith(request.getRequestUri().getPath(), getAdminRequiredPrefixes())) return principal.isAdmin();
        return true;
    }

}
