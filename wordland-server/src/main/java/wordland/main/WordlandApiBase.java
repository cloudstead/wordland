package wordland.main;

import com.fasterxml.jackson.databind.JsonNode;
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.main.MainApiBase;
import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.ApiConstants.*;

public abstract class WordlandApiBase<OPT extends WordlandApiOptionsBase> extends MainApiBase<OPT> {

    @Override protected ApiClientBase initApiClient() {
        final ApiConnectionInfo info = new ApiConnectionInfo();
        info.setBaseUri(wordlandEnv().get(ENV_API_SERVER));
        return new ApiClientBase(info) { @Override public String getTokenHeader() { return API_TOKEN; } };
    }

    @Override protected Object buildLoginRequest(OPT options) {
        if (options.hasAccount() && options.hasPassword()) {
            return new LoginRequest(options.getAccount(), options.getPassword());
        }
        final Map<String, String> env = wordlandEnv();
        return options.hasAccount()
                ? new LoginRequest(options.getAccount(),   env.get(ENV_SUPERUSER_PASS))
                : new LoginRequest(env.get(ENV_SUPERUSER), env.get(ENV_SUPERUSER_PASS));
    }

    @Override protected String getApiHeaderTokenName() { return API_TOKEN; }

    @Override protected String getLoginUri(String account) { return AUTH_ENDPOINT+EP_LOGIN; }

    @Override protected String getSessionId(RestResponse response) throws Exception {
        return json(response.json, JsonNode.class).get(FIELD_API_TOKEN).textValue();
    }

    @Override protected void setSecondFactor(Object loginRequest, String token) { }

}
