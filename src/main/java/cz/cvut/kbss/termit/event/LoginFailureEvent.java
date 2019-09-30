package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.UserAccount;

/**
 * Emitted when a user login attempt fails.
 */
public class LoginFailureEvent extends UserEvent {

    public LoginFailureEvent(UserAccount user) {
        super(user);
    }
}
