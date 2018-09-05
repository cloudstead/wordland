package wordland.model.game;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TileFunctionException extends RuntimeException {
    public TileFunctionException (String message) { super(message); }
    public TileFunctionException (String message, Throwable t) { super(message, t); }
    public TileFunctionException (Throwable t) { super(t); }
}
