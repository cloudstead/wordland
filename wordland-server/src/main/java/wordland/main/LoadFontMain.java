package wordland.main;

import org.cobbzilla.wizard.client.ApiClientBase;

import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.ApiConstants.SYMBOL_FONTS_ENDPOINT;

public class LoadFontMain extends WordlandApiBase<LoadFontOptions> {

    public static void main (String[] args) { main(LoadFontMain.class, args); }

    @Override protected void run() throws Exception {
        final LoadFontOptions options = getOptions();
        final ApiClientBase api = getApiClient();
        final String json = json(options.getFontRequest());
        out(options.isUpdate() ? api.post(SYMBOL_FONTS_ENDPOINT +"/"+options.name(), json) : api.put(SYMBOL_FONTS_ENDPOINT, json));
    }

}
