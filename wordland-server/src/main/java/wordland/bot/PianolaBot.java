package wordland.bot;

import lombok.Getter;
import org.cobbzilla.wizard.util.TestNames;
import wordland.bot.strategy.PianolaRoundRobinStrategy;
import wordland.bot.strategy.PianolaTimedTurnStrategy;
import wordland.dao.SessionDAO;
import wordland.model.game.GamePlayer;
import wordland.model.support.AccountSession;
import wordland.server.WordlandConfiguration;
import wordland.service.GameDaemon;

import static org.cobbzilla.wizard.model.StrongIdentifiableBase.newStrongUuid;

public class PianolaBot {

    @Getter private GameDaemon gameDaemon;
    @Getter private WordlandConfiguration configuration;

    @Getter private GamePlayer player;
    @Getter private PianolaBotStrategy botStrategy;

    public PianolaBot (GameDaemon daemon, WordlandConfiguration config, GamePlayer player) {
        this.gameDaemon = daemon;
        this.configuration = config;
        this.player = player != null ? player : new GamePlayer()
                .setId(newStrongUuid())
                .setBot(true)
                .setName("~ "+ TestNames.animal().replace("_", " "));
        if (player == null) {
            final String apiToken = config.getBean(SessionDAO.class).create(new AccountSession(this.player.getName()));
            daemon.addPlayer(this.player.setApiToken(apiToken));
        }
    }

    public PianolaBot start() {
        if (gameDaemon.getRoom().getSettings().hasRoundRobinPolicy()) {
            botStrategy = new PianolaRoundRobinStrategy(this);
        } else {
            botStrategy = new PianolaTimedTurnStrategy(this);
        }
        botStrategy.start();
        return this;
    }

}
