package cz.cvut.kbss.termit.security;

/**
 * Security-related constants.
 */
public class SecurityConstants {

    /**
     * Cookie used for the remember-me function
     */
    public static final String REMEMBER_ME_COOKIE_NAME = "remember-me";

    /**
     * Username parameter for the login form
     */
    public static final String USERNAME_PARAM = "username";

    /**
     * Password parameter for the login form
     */
    public static final String PASSWORD_PARAM = "password";

    /**
     * URL used for logging into the application
     */
    public static final String SECURITY_CHECK_URI = "/j_spring_security_check";

    /**
     * Request/response header used to store authentication info.
     */
    public static final String AUTHENTICATION_HEADER = "Authentication";

    /**
     * String prefix added to JWT tokens in the {@link #AUTHENTICATION_HEADER}.
     */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * JWT claim used to store user's global roles in the system.
     */
    public static final String JWT_ROLE_CLAIM = "role";

    /**
     * Delimiter used to separate roles in a JWT.
     */
    public static final String JWT_ROLE_DELIMITER = "-";

    /**
     * Session timeout in milliseconds. 10 minutes.
     */
    public static final int SESSION_TIMEOUT = 10 * 60 * 1000;

    /**
     * Maximum number of unsuccessful login attempts.
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * System administrator role
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Regular system user role
     */
    public static final String ROLE_USER = "ROLE_USER";

    private SecurityConstants() {
        throw new AssertionError();
    }
}
