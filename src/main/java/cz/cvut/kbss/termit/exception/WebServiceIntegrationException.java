package cz.cvut.kbss.termit.exception;

/**
 * Indicates that a call to a remote web service has failed.
 */
public class WebServiceIntegrationException extends TermItException {

    public WebServiceIntegrationException(String message) {
        super(message);
    }

    public WebServiceIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
