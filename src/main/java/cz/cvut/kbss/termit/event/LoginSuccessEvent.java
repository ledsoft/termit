package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.UserAccount;

/**
 * Emitted when a user successfully logs in.
 */
public class LoginSuccessEvent extends UserEvent {

    public LoginSuccessEvent(UserAccount user) {
        super(user);
    }
}
