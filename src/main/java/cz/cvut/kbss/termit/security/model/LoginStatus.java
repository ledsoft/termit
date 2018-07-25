package cz.cvut.kbss.termit.security.model;

/**
 * Value object for sending login status info to the user after a login/logout attempt.
 */
public class LoginStatus {

    private boolean loggedIn;
    private String username;
    private String errorMessage;
    /**
     * Represents identifier of the error, which can be resolved to a localized message in the JS UI
     */
    private String errorId;
    private boolean success;

    public LoginStatus() {
    }

    public LoginStatus(boolean loggedIn, boolean success, String username, String errorMessage) {
        this.loggedIn = loggedIn;
        this.username = username;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
