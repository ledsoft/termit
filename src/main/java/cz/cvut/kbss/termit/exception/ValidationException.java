package cz.cvut.kbss.termit.exception;

import cz.cvut.kbss.termit.util.ValidationResult;

import java.util.stream.Collectors;

/**
 * Indicates that invalid data have been passed to the application.
 * <p>
 * The exception message should provide information as to what data are invalid and why.
 */
public class ValidationException extends TermItException {

    private ValidationResult<?> validationResult;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(ValidationResult<?> validationResult) {
        assert !validationResult.isValid();
        this.validationResult = validationResult;
    }

    @Override
    public String getMessage() {
        if (validationResult == null) {
            return super.getMessage();
        }
        return String.join("\n",
                validationResult.getViolations().stream()
                                .map(cv -> "Value of " + cv.getRootBeanClass().getSimpleName() + "." +
                                        cv.getPropertyPath() + " " + cv.getMessage())
                                .collect(Collectors.toSet()));
    }
}
