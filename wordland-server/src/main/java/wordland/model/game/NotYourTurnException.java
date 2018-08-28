package wordland.model.game;

import org.cobbzilla.wizard.validation.SimpleViolationException;

public class NotYourTurnException extends SimpleViolationException {

    public NotYourTurnException () { super("err.game.notYourTurn"); }

}
