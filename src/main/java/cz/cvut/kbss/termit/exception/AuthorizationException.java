package cz.cvut.kbss.termit.exception;

/**
 * Indicates that a the user attempted to access a resources/function for which they have insufficient authority.
 */
public class AuthorizationException extends TermItException {

    public AuthorizationException(String message) {
        super(message);
    }
}
