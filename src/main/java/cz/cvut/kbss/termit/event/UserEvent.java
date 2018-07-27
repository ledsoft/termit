package cz.cvut.kbss.termit.event;

import cz.cvut.kbss.termit.model.User;

import java.util.Objects;

/**
 * Base class for user-related events.
 */
abstract class UserEvent {

    private final User user;

    UserEvent(User user) {
        this.user = Objects.requireNonNull(user);
    }

    /**
     * Gets the user who is concerned by this event.
     *
     * @return User
     */
    public User getUser() {
        return user;
    }
}
