package wordland;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.client.ApiClientBase;
import wordland.server.WordlandConfiguration;

import static wordland.ApiConstants.API_TOKEN;

public class WordlandApiClient extends ApiClientBase {

    @Getter @Setter private WordlandConfiguration configuration;

    public WordlandApiClient (WordlandConfiguration configuration) {
        super(configuration.getApiUriBase());
        this.configuration = configuration;
    }

    @Override public String getTokenHeader() { return API_TOKEN; }

}
