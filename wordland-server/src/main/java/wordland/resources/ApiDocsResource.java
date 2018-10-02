package wordland.resources;

import lombok.Getter;
import org.cobbzilla.wizard.resources.OpenApiDocsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.server.WordlandConfiguration;

import javax.ws.rs.Path;

import static lombok.AccessLevel.PROTECTED;

@Path("/docs") @Service
public class ApiDocsResource extends OpenApiDocsResource {

    @Getter @Autowired private WordlandConfiguration configuration;
    @Getter(PROTECTED) private final String[] packagesToScan = {"wordland.resources"};

}
