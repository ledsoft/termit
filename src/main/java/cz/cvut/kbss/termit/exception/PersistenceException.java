package cz.cvut.kbss.termit.exception;

/**
 * Marks an exception that occurred in the persistence layer.
 */
public class PersistenceException extends TermItException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(Throwable cause) {
        super(cause);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
