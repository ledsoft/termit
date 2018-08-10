package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.AuthorizationException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepositoryService sut;

    @Test
    void existsByUsernameReturnsTrueForExistingUsername() {
        final User user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));

        assertTrue(sut.exists(user.getUsername()));
    }

    @Test
    void persistGeneratesIdentifierForUser() {
        final User user = Generator.generateUser();
        sut.persist(user);
        assertNotNull(user.getUri());

        final User result = em.find(User.class, user.getUri());
        assertNotNull(result);
        assertEquals(user, result);
    }

    @Test
    void persistEncodesUserPassword() {
        final User user = Generator.generateUserWithId();
        final String plainPassword = user.getPassword();

        sut.persist(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsNull() {
        final User user = Generator.generateUserWithId();
        user.setPassword(null);
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be empty"));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsEmpty() {
        final User user = Generator.generateUserWithId();
        user.setPassword("");
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be empty"));
    }

    @Test
    void updateEncodesPasswordWhenItWasChanged() {
        final User user = persistUser();
        Environment.setCurrentUser(user);
        final String plainPassword = "updatedPassword01";
        user.setPassword(plainPassword);

        sut.update(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void updateRetainsOriginalPasswordWhenItDoesNotChange() {
        final User user = Generator.generateUserWithId();
        final String plainPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
        user.setPassword(null); // Simulate instance being loaded from repo
        final String newLastName = "newLastName";
        user.setLastName(newLastName);

        sut.update(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
        assertEquals(newLastName, result.getLastName());
    }

    @Test
    void updateThrowsNotFoundExceptionWhenUserDoesNotExist() {
        final User user = Generator.generateUserWithId();
        Environment.setCurrentUser(user);
        final NotFoundException ex = assertThrows(NotFoundException.class, () -> sut.update(user));
        assertEquals("User " + user + " does not exist.", ex.getMessage());
    }

    @Test
    void postLoadErasesPasswordFromInstance() {
        final User user = persistUser();

        final Optional<User> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertNull(result.get().getPassword());
    }

    private User persistUser() {
        final User user = Generator.generateUserWithId();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
        return user;
    }

    @Test
    void updateThrowsValidationExceptionWhenUpdatedInstanceIsMissingValues() {
        final User user = persistUser();
        Environment.setCurrentUser(user);

        user.setUsername(null);
        user.setPassword(null); // Simulate instance being loaded from repo
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.update(user));
        assertThat(ex.getMessage(), containsString("username must not be empty"));
    }

    @Test
    void locksUserAccountWhenLoginLimitExceededEventIsReceived() {
        final User user = persistUser();
        assertFalse(user.isLocked());
        final LoginAttemptsThresholdExceeded event = new LoginAttemptsThresholdExceeded(user);
        sut.onLoginAttemptsThresholdExceeded(event);

        final User result = em.find(User.class, user.getUri());
        assertTrue(result.isLocked());
    }

    @Test
    void unlockRemovesLockedClassFromUserAndSetsHimNewPassword() {
        final User user = Generator.generateUserWithId();
        user.lock();
        transactional(() -> em.persist(user));
        final String newPassword = "newPassword";

        sut.unlock(user, newPassword);
        final User result = em.find(User.class, user.getUri());
        assertFalse(result.isLocked());
        assertTrue(passwordEncoder.matches(newPassword, result.getPassword()));
    }

    @Test
    void disableDisablesUserAccount() {
        final User user = persistUser();
        assertTrue(user.isEnabled());

        sut.disable(user);
        final User result = em.find(User.class, user.getUri());
        assertFalse(result.isEnabled());
    }

    @Test
    void enableEnablesDisabledUserAccount() {
        final User user = Generator.generateUserWithId();
        user.disable();
        transactional(() -> em.persist(user));
        assertFalse(user.isEnabled());

        sut.enable(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(result.isEnabled());
    }

    @Test
    void updateThrowsAuthorizationExceptionWhenUserAttemptsToUpdateAnotherUser() {
        final User user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
        final User toUpdate = Generator.generateUserWithId();

        final AuthorizationException ex = assertThrows(AuthorizationException.class, () -> sut.update(toUpdate));
        assertEquals("User " + user + " attempted to update a different user's account.", ex.getMessage());
    }

    @Test
    void persistAddsUserRestrictedType() {
        final User user = Generator.generateUser();
        sut.persist(user);

        final User result = em.find(User.class, user.getUri());
        assertTrue(result.getTypes().contains(Vocabulary.s_c_restricted_user));
    }

    @Test
    void persistEnsuresAdminTypeIsNotPresentInUserAccount() {
        final User user = Generator.generateUser();
        user.addType(Vocabulary.s_c_admin);
        sut.persist(user);

        final User result = em.find(User.class, user.getUri());
        assertFalse(result.getTypes().contains(Vocabulary.s_c_admin));
    }
}