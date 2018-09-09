package wordland.model.game;

import org.cobbzilla.wizard.validation.SimpleViolationException;

public class PlayRateLimitException extends SimpleViolationException {

    public PlayRateLimitException(String limitName) { super("err.game.slowYourRoll", "word play rate limit exceeded", limitName); }

}
