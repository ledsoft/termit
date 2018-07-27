package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.User;

/**
 * Emitted when a user successfully logs in.
 */
public class LoginSuccessEvent extends UserEvent {

    public LoginSuccessEvent(User user) {
        super(user);
    }
}
