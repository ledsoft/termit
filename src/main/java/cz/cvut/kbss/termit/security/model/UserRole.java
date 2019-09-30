package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.util.Vocabulary;

import static cz.cvut.kbss.termit.security.SecurityConstants.ROLE_ADMIN;
import static cz.cvut.kbss.termit.security.SecurityConstants.ROLE_USER;

/**
 * Represents user roles in the system.
 * <p>
 * These roles are used for basic system authorization.
 */
public enum UserRole {

    /**
     * Regular application user.
     * <p>
     * Does not map to any specific subclass of {@link Vocabulary#s_c_uzivatel_termitu}.
     */
    USER("", ROLE_USER),
    /**
     * Application administrator.
     * <p>
     * Maps to {@link Vocabulary#s_c_administrator_termitu}.
     */
    ADMIN(Vocabulary.s_c_administrator_termitu, ROLE_ADMIN);

    private final String type;
    private final String name;

    UserRole(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Checks whether a role for the specified type exists.
     *
     * @param type The type to check
     * @return Role existence info
     */
    public static boolean exists(String type) {
        for (UserRole r : values()) {
            if (r.type.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets role for the specified ontological type.
     *
     * @param type Type to get role for
     * @return Matching role
     * @throws IllegalArgumentException If no matching role exists
     */
    public static UserRole fromType(String type) {
        for (UserRole r : values()) {
            if (r.type.equals(type)) {
                return r;
            }
        }
        throw new IllegalArgumentException("No role found for type " + type + ".");
    }
}
