package cz.cvut.kbss.termit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates that a user's authentication token has expired.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenExpiredException extends JwtException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
