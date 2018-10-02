package wordland.server.listener;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;
import wordland.model.GameBoard;
import wordland.model.SymbolSet;
import wordland.server.WordlandConfiguration;
import wordland.service.AtmosphereEventsService;

@Slf4j
public class AtmosphereLifecycleListener extends RestServerLifecycleListenerBase<WordlandConfiguration> {

    private static final Class<? extends Identifiable>[] SEED_CLASSES = new Class[]{
            SymbolSet.class,
            GameBoard.class
    };

    @Getter private WordlandConfiguration configuration;

    @Override public void onStart(RestServer server) {

        this.configuration = (WordlandConfiguration) server.getConfiguration();

        // start nettosphere event server
        final Nettosphere nettosphere = new Nettosphere.Builder().config(
                new Config.Builder()
                        .host("0.0.0.0")
                        .port(configuration.getAtmospherePort())
                        .resource(AtmosphereEventsService.class)
                        .build())
                .build();
        nettosphere.start();
    }

}
