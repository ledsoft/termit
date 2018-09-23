package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest extends BaseServiceTestRunner {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private UserAccountDao userAccountDao;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        this.user = generateAccount();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsCurrentlyLoggedInUser() {
        Environment.setCurrentUser(user);
        final UserAccount result = securityUtils.getCurrentUser();
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
        final UserAccount update = new UserAccount();
        update.setUri(Generator.generateUri());
        update.setFirstName("updatedFirstName");
        update.setLastName("updatedLastName");
        update.setPassword(user.getPassword());
        update.setUsername(user.getUsername());
        transactional(() -> userAccountDao.update(update));
        securityUtils.updateCurrentUser();

        final UserAccount currentUser = securityUtils.getCurrentUser();
        assertEquals(update, currentUser);
    }

    @Test
    void verifyCurrentUserPasswordThrowsIllegalArgumentWhenPasswordDoesNotMatch() {
        Environment.setCurrentUser(user);
        final String password = "differentPassword";
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> securityUtils.verifyCurrentUserPassword(password));
        assertThat(ex.getMessage(), containsString("does not match"));

    }
}