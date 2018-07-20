package cz.cvut.kbss.termit.security;

/**
 * Security-related constants.
 */
public class SecurityConstants {

    /**
     * Name of the cookie holding user session info.
     */
    public static final String SESSION_COOKIE_NAME = "TermIt_JSESSIONID";

    /**
     * Cookie used for the remember-me function
     */
    public static final String REMEMBER_ME_COOKIE_NAME = "remember-me";

    /**
     * CSRF protection cookie
     */
    public static final String CSRF_COOKIE_NAME = "CSRF-TOKEN";

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
     * URL used for logging out of the application
     */
    public static final String LOGOUT_URI = "/j_spring_security_logout";

    /**
     * Base URI for the application cookies
     */
    public static final String COOKIE_URI = "/";

    /**
     * Session timeout in seconds. 30 minutes.
     */
    public static final int SESSION_TIMEOUT = 30 * 60;

    /**
     * Maximum number of unsuccessful login attempts.
     */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
}
