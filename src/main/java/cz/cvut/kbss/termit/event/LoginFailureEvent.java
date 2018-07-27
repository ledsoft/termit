package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.User;

/**
 * Emitted when a user login attempt fails.
 */
public class LoginFailureEvent extends UserEvent {

    public LoginFailureEvent(User user) {
        super(user);
    }
}
