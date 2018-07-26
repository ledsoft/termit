package cz.cvut.kbss.termit.exception;

/**
 * Indicates that a user's authentication token has expired.
 */
public class TokenExpiredException extends JwtException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
