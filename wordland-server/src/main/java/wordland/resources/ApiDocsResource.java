package wordland.resources;

import lombok.Getter;
import org.cobbzilla.wizard.resources.OpenApiDocsResource;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

import static lombok.AccessLevel.PROTECTED;

@Path("/docs") @Service
public class ApiDocsResource extends OpenApiDocsResource {

    @Getter(PROTECTED) private final String[] packagesToScan = {"wordland.resources"};

}
