package cz.cvut.kbss.termit.rest.handler;

import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.exception.UsernameExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * Exception handlers for REST controllers.
 * <p>
 * The general pattern should be that unless an exception can be handled in a more appropriate place it bubbles up to a
 * REST controller which originally received the request. There, it is caught by this handler, logged and a reasonable
 * error message is returned to the user.
 */
@ControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private void logException(RuntimeException ex) {
        logException("Exception caught.", ex);
    }

    private void logException(String message, RuntimeException ex) {
        LOG.error(message, ex);
    }

    private ErrorInfo errorInfo(HttpServletRequest request, Throwable e) {
        return new ErrorInfo(e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ErrorInfo> persistenceException(HttpServletRequest request, PersistenceException e) {
        logException(e);
        return new ResponseEntity<>(errorInfo(request, e.getCause()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(OWLPersistenceException.class)
    public ResponseEntity<ErrorInfo> jopaException(HttpServletRequest request, OWLPersistenceException e) {
        logException("Persistence exception caught.", e);
        return new ResponseEntity<>(errorInfo(request, e), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<ErrorInfo> usernameExistsException(HttpServletRequest request, UsernameExistsException e) {
        logException(e);
        return new ResponseEntity<>(errorInfo(request, e), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorInfo> resourceNotFound(HttpServletRequest request, NotFoundException e) {
        return new ResponseEntity<>(errorInfo(request, e), HttpStatus.NOT_FOUND);
    }
}