package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.event.LoginFailureEvent;
import cz.cvut.kbss.termit.event.LoginSuccessEvent;

/**
 * Dummy login tracker which does nothing.
 */
public class DisabledLoginTracker implements LoginTracker {

    @Override
    public void onLoginFailure(LoginFailureEvent event) {
        // Do nothing
    }

    @Override
    public void onLoginSuccess(LoginSuccessEvent event) {
        // Do nothing
    }
}
