package cz.cvut.kbss.termit.exception;

/**
 * Thrown when trying to add an user whose username already exists in the repository.
 */
public class UsernameExistsException extends TermItException {

    public UsernameExistsException(String message) {
        super(message);
    }
}
