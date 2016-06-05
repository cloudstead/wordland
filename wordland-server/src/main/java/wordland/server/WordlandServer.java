package wordland.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;
import java.util.Map;

@Slf4j
public class WordlandServer extends RestServerBase<WordlandConfiguration> {

    public static final String[] API_CONFIG_YML = {"wordland-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {

        final List<ConfigurationSource> configSources = getConfigurationSources();

        Map<String, String> env = System.getenv();
        if (env.get("WORDLAND_DATAKEY") == null) {
            // use defaults
            env = CommandShell.loadShellExports(".wordland.env");
        }

        // todo: in a clustered environment, only 1 server needs to seed the DB upon startup
        main(WordlandServer.class, new DbSeedListener(), configSources, env);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(WordlandServer.class, API_CONFIG_YML);
    }
}
