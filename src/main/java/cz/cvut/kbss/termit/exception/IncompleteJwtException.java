package cz.cvut.kbss.termit.exception;

/**
 * Indicates that the specified JWT does not contain all the required data.
 */
public class IncompleteJwtException extends JwtException {

    public IncompleteJwtException(String message) {
        super(message);
    }
}
