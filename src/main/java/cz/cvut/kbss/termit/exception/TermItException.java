package cz.cvut.kbss.termit.exception;

/**
 * Application-specific exception.
 * <p>
 * All exceptions related to the application should be subclasses of this one.
 */
public class TermItException extends RuntimeException {

    protected TermItException() {
    }

    public TermItException(String message) {
        super(message);
    }

    public TermItException(String message, Throwable cause) {
        super(message, cause);
    }

    public TermItException(Throwable cause) {
        super(cause);
    }
}
