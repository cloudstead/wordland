package wordland.server;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.NamedIdentityBase;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;
import wordland.dao.*;
import wordland.model.*;
import wordland.model.json.GameRoomSettings;
import wordland.service.AtmosphereEventsService;
import wordland.service.GamesMaster;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;
import static wordland.ApiConstants.STANDARD;

@Slf4j
public class WordlandLifecycleListener extends RestServerLifecycleListenerBase<WordlandConfiguration> {

    private static final Class<? extends Identifiable>[] SEED_CLASSES = new Class[]{
            SymbolSet.class,
            SymbolDistribution.class,
            PointSystem.class,
            GameBoard.class
    };
    private static final long INIT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

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

        seed(server, false);
    }

    public void seed(RestServer server, boolean includeTestData) {

        final WordlandConfiguration configuration = (WordlandConfiguration) server.getConfiguration();

        for (Class<? extends Identifiable> seedClass : SEED_CLASSES) populate(configuration, seedClass);

        if (includeTestData || Boolean.valueOf(System.getenv("WORDLAND_CREATE_SUPERUSER"))) {
            final Map<String, String> env = server.getConfiguration().getEnvironment();
            if (env.containsKey("WORDLAND_SUPERUSER") && env.containsKey("WORDLAND_SUPERUSER_PASS")) {
                final AccountDAO accountDAO = configuration.getBean(AccountDAO.class);
                if (accountDAO.adminsExist()) {
                    log.info("Admin accounts already exist, not creating new superuser");
                } else {
                    final String name = env.get("WORDLAND_SUPERUSER");
                    final Account created = accountDAO.create((Account) new Account()
                            .setAnonymous(false)
                            .setFirstName("admin")
                            .setLastName("admin")
                            .setMobilePhone("0000000000")
                            .setMobilePhoneCountryCode(1)
                            .setHashedPassword(new HashedPassword(env.get("WORDLAND_SUPERUSER_PASS")))
                            .setAccountName(name)
                            .setAdmin(true)
                            .setEmail(name)
                            .setName(name));
                    log.info("Created superuser account " + name + ": " + created.getUuid());
                }
            }
        }

        final GameRoomDAO roomDAO = configuration.getBean(GameRoomDAO.class);
        final List<GameRoom> rooms = roomDAO.findAll();
        if (rooms.isEmpty()) {
            final GameRoomSettings roomSettings = new GameRoomSettings()
                    .setBoard(configuration.getBean(GameBoardDAO.class).findByName(STANDARD))
                    .setSymbolSet(configuration.getBean(SymbolSetDAO.class).findByName(STANDARD))
                    .setPointSystem(configuration.getBean(PointSystemDAO.class).findByName(STANDARD))
                    .setDefaultDistribution(configuration.getBean(SymbolDistributionDAO.class).findByName(STANDARD));
            configuration.getBean(GameRoomDAO.class).create(new GameRoom(STANDARD).setSettings(roomSettings));
        } else {
            final GamesMaster gamesMaster = configuration.getBean(GamesMaster.class);
            for (GameRoom room : rooms) gamesMaster.initRoom(room);
        }
        super.onStart(server);
    }

    public void populate(WordlandConfiguration configuration, Class<? extends Identifiable> type) {
        final DAO dao = configuration.getDaoForEntityType(type);
        final Identifiable[] things = (Identifiable[]) fromJsonOrDie(loadResourceAsStringOrDie("seed/" + type.getSimpleName() + ".json"), arrayClass(type));
        if (dao instanceof NamedIdentityBaseDAO) {
            final NamedIdentityBaseDAO nameDAO = (NamedIdentityBaseDAO) dao;
            for (Identifiable thing : things) {
                final NamedIdentityBase named = (NamedIdentityBase) thing;
                if (nameDAO.findByName(named.getName()) == null) nameDAO.create(named);
            }
        } else {
            die("populate: DAO is not a NamedIdentityBaseDAO: "+dao.getClass().getName());
        }
    }

}
