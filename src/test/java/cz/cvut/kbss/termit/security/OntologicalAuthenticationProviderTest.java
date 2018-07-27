package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestSecurityConfig;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
@ContextConfiguration(classes = {TestSecurityConfig.class})
class OntologicalAuthenticationProviderTest extends BaseServiceTestRunner {

    @Autowired
    private AuthenticationProvider provider;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private String plainPassword;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        this.plainPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(plainPassword));
        transactional(() -> userDao.persist(user));
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @Test
    void successfulAuthenticationSetsSecurityContext() {
        final Authentication auth = authentication(user.getUsername(), plainPassword);
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
        final Authentication result = provider.authenticate(auth);
        assertNotNull(SecurityContextHolder.getContext());
        final UserDetails details = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertEquals(user.getUsername(), details.getUsername());
        assertTrue(result.isAuthenticated());
    }

    private static Authentication authentication(String username, String password) {
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    @Test
    void authenticateThrowsUserNotFoundExceptionForUnknownUsername() {
        final Authentication auth = authentication("unknownUsername", user.getPassword());
        assertThrows(UsernameNotFoundException.class, () -> provider.authenticate(auth));
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
    }

    @Test
    void authenticateThrowsBadCredentialsForInvalidPassword() {
        final Authentication auth = authentication(user.getUsername(), "unknownPassword");
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
        final SecurityContext context = SecurityContextHolder.getContext();
        assertNull(context.getAuthentication());
    }

    @Test
    void supportsUsernameAndPasswordAuthentication() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateThrowsAuthenticationExceptionForEmptyUsername() {
        final Authentication auth = authentication("", "");
        final UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> provider.authenticate(auth));
        assertThat(ex.getMessage(), containsString("Username cannot be empty."));
    }
}