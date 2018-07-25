package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest extends BaseServiceTestRunner {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private UserDao userDao;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUser();
        user.setUri(Generator.generateUri());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsCurrentlyLoggedInUser() {
        Environment.setCurrentUser(user);
        final User result = securityUtils.getCurrentUser();
        assertEquals(user, result);
    }

    @Test
    void getCurrentUserDetailsReturnsUserDetailsOfCurrentlyLoggedInUser() {
        Environment.setCurrentUser(user);
        final Optional<UserDetails> result = securityUtils.getCurrentUserDetails();
        assertTrue(result.isPresent());
        assertTrue(result.get().isEnabled());
        assertEquals(user, result.get().getUser());
    }

    @Test
    void getCurrentUserDetailsReturnsEmptyOptionalWhenNoUserIsLoggedIn() {
        assertFalse(securityUtils.getCurrentUserDetails().isPresent());
    }

    @Test
    void updateCurrentUserReplacesUserInCurrentSecurityContext() {
        Environment.setCurrentUser(user);
        final User update = new User();
        update.setUri(Generator.generateUri());
        update.setFirstName("updatedFirstName");
        update.setLastName("updatedLastName");
        update.setPassword(user.getPassword());
        update.setUsername(user.getUsername());
        transactional(() -> userDao.update(update));
        securityUtils.updateCurrentUser();

        final User currentUser = securityUtils.getCurrentUser();
        assertEquals(update, currentUser);
    }

    @Test
    void verifyCurrentUserPasswordThrowsIllegalArgumentWhenPasswordDoesNotMatch() {
        Environment.setCurrentUser(user);
        final String password = "differentPassword";
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> securityUtils.verifyCurrentUserPassword(password));
        assertThat(ex.getMessage(), containsString("does not match"));

    }
}