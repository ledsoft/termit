package cz.cvut.kbss.termit.exception;

/**
 * Indicates a failure during document annotation generation.
 */
public class AnnotationGenerationException extends TermItException {

    public AnnotationGenerationException(String message) {
        super(message);
    }

    public AnnotationGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
