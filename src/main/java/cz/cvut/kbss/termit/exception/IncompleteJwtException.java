package cz.cvut.kbss.termit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates that the specified JWT does not contain all the required data.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IncompleteJwtException extends JwtException {

    public IncompleteJwtException(String message) {
        super(message);
    }
}
