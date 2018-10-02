package wordland.resources.docs;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonSet;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/docs")
@Produces(APPLICATION_JSON)
@Service @Slf4j
public class ApiDocsResource {

    private static final String PACKAGE_TO_SCAN = "wordland.resources";

    @Getter(lazy=true) private final OpenAPI swagger = initSwagger();

    protected OpenAPI initSwagger() {
        final JaxrsAnnotationScanner scanner = new JaxrsAnnotationScanner();
        scanner.setConfiguration(new OpenAPIConfiguration() {
            @Override public Set<String> getResourcePackages() { return new SingletonSet<>(PACKAGE_TO_SCAN); }

            @Override public Set<String> getResourceClasses() {
                final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(Path.class));
                return scanner.findCandidateComponents(PACKAGE_TO_SCAN).stream()
                        .map(BeanDefinition::getBeanClassName)
                        .collect(Collectors.toSet());
            }

            @Override public String getReaderClass() { return Reader.class.getName(); }

            @Override public String getScannerClass() { return scanner.getClass().getName(); }

            @Override public String getFilterClass() { return null; }

            @Override public Collection<String> getIgnoredRoutes() { return null; }

            @Override public OpenAPI getOpenAPI() { return getSwagger(); }

            @Override public Map<String, Object> getUserDefinedOptions() { return Collections.emptyMap(); }

            @Override public Boolean isReadAllResources() { return true; }

            @Override public Boolean isPrettyPrint() { return true; }

            @Override public Long getCacheTTL() { return TimeUnit.DAYS.toMillis(1); }
        });

        final OpenAPI api = new OpenAPI();
        final Reader reader = new Reader(api);
        final Set<Class<?>> classes = scanner.classes();
        return reader.read(classes);
    }

    @GET @Path("/swagger.json")
    @Operation(description = "The swagger definition in JSON", hidden = true)
    public Response getListingJson() { return Response.ok().entity(getSwagger()).build(); }

}
