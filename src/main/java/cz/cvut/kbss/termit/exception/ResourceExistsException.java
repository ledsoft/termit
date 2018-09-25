package cz.cvut.kbss.termit.exception;

/**
 * Thrown when trying to add an instance with identifier which already exists in the repository.
 */
public class ResourceExistsException extends TermItException {

    public ResourceExistsException(String message) {
        super(message);
    }

    public static ResourceExistsException create(String resource, Object identifier) {
        return new ResourceExistsException(resource + " with identifier " + identifier + " already exists.");
    }

    public static ResourceExistsException create(String message) {
        return new ResourceExistsException(message);
    }
}
