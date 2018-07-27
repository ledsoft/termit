package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.User;

/**
 * Event emitted when a user exceeds the maximum number ({@link cz.cvut.kbss.termit.security.SecurityConstants#MAX_LOGIN_ATTEMPTS})
 * of unsuccessful login attempts.
 */
public class LoginAttemptsThresholdExceeded extends UserEvent {

    public LoginAttemptsThresholdExceeded(User user) {
        super(user);
    }
}
