package wordland.server;

import edu.emory.mathcs.backport.java.util.Arrays;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.RestServerLifecycleListener;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.network.NetworkUtil.IPv4_ALL_ADDRS;
import static wordland.ApiConstants.WL_SERVER_ENV_FILE;

@NoArgsConstructor @Slf4j
public class WordlandServer extends RestServerBase<WordlandConfiguration> {

    public static final String[] API_CONFIG_YML = {"wordland-config.yml"};
    public static final WordlandLifecycleListener WORDLAND_LIFECYCLE_LISTENER = new WordlandLifecycleListener();

    private static final List<RestServerLifecycleListener> WORDLAND_LIFECYCLE_LISTENERS = Arrays.asList(new RestServerLifecycleListener[]{
            WORDLAND_LIFECYCLE_LISTENER
    });

    //    @Override protected String getListenAddress() { return LOCALHOST; }
    @Override protected String getListenAddress() { return IPv4_ALL_ADDRS; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {

        final List<ConfigurationSource> configSources = getConfigurationSources();

        Map<String, String> env = System.getenv();
        if (env.get("WORDLAND_DATAKEY") == null) {
            // use defaults
            env = CommandShell.loadShellExports(WL_SERVER_ENV_FILE);
        }

        // todo: in a clustered environment, only 1 server needs to seed the DB upon startup
        main(args, WordlandServer.class, WORDLAND_LIFECYCLE_LISTENERS, configSources, env);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(WordlandServer.class, API_CONFIG_YML);
    }
}
