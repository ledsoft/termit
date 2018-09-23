package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.exception.AuthorizationException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.util.Optional;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
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
        final UserAccount user = generateAccount();
        transactional(() -> em.persist(user));

        assertTrue(sut.exists(user.getUsername()));
    }

    @Test
    void persistGeneratesIdentifierForUser() {
        final UserAccount user = generateAccount();
        user.setUri(null);
        sut.persist(user);
        assertNotNull(user.getUri());

        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertNotNull(result);
        assertEquals(user, result);
    }

    @Test
    void persistEncodesUserPassword() {
        final UserAccount user = generateAccount();
        final String plainPassword = user.getPassword();

        sut.persist(user);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsNull() {
        final UserAccount user = generateAccount();
        user.setPassword(null);
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be blank"));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsEmpty() {
        final UserAccount user = generateAccount();
        user.setPassword("");
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be blank"));
    }

    @Test
    void updateEncodesPasswordWhenItWasChanged() {
        final UserAccount user = persistUser();
        Environment.setCurrentUser(user);
        final String plainPassword = "updatedPassword01";
        user.setPassword(plainPassword);

        sut.update(user);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void updateRetainsOriginalPasswordWhenItDoesNotChange() {
        final UserAccount user = generateAccount();
        final String plainPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
        user.setPassword(null); // Simulate instance being loaded from repo
        final String newLastName = "newLastName";
        user.setLastName(newLastName);

        sut.update(user);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
        assertEquals(newLastName, result.getLastName());
    }

    @Test
    void updateThrowsNotFoundExceptionWhenUserDoesNotExist() {
        final UserAccount user = generateAccount();
        Environment.setCurrentUser(user);
        final NotFoundException ex = assertThrows(NotFoundException.class, () -> sut.update(user));
        assertEquals("User " + user + " does not exist.", ex.getMessage());
    }

    @Test
    void postLoadErasesPasswordFromInstance() {
        final UserAccount user = persistUser();

        final Optional<UserAccount> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertNull(result.get().getPassword());
    }

    private UserAccount persistUser() {
        final UserAccount user = generateAccount();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
        return user;
    }

    @Test
    void updateThrowsValidationExceptionWhenUpdatedInstanceIsMissingValues() {
        final UserAccount user = persistUser();
        Environment.setCurrentUser(user);

        user.setUsername(null);
        user.setPassword(null); // Simulate instance being loaded from repo
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.update(user));
        assertThat(ex.getMessage(), containsString("username must not be blank"));
    }

    @Test
    void locksUserAccountWhenLoginLimitExceededEventIsReceived() {
        final UserAccount user = persistUser();
        assertFalse(user.isLocked());
        final LoginAttemptsThresholdExceeded event = new LoginAttemptsThresholdExceeded(user);
        sut.onLoginAttemptsThresholdExceeded(event);

        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(result.isLocked());
    }

    @Test
    void unlockRemovesLockedClassFromUserAndSetsHimNewPassword() {
        final UserAccount user = generateAccount();
        user.lock();
        transactional(() -> em.persist(user));
        final String newPassword = "newPassword";

        sut.unlock(user, newPassword);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertFalse(result.isLocked());
        assertTrue(passwordEncoder.matches(newPassword, result.getPassword()));
    }

    @Test
    void disableDisablesUserAccount() {
        final UserAccount user = persistUser();
        assertTrue(user.isEnabled());

        sut.disable(user);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertFalse(result.isEnabled());
    }

    @Test
    void enableEnablesDisabledUserAccount() {
        final UserAccount user = generateAccount();
        user.disable();
        transactional(() -> em.persist(user));
        assertFalse(user.isEnabled());

        sut.enable(user);
        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(result.isEnabled());
    }

    @Test
    void updateThrowsAuthorizationExceptionWhenUserAttemptsToUpdateAnotherUser() {
        final UserAccount user = generateAccount();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
        final UserAccount toUpdate = generateAccount();

        final AuthorizationException ex = assertThrows(AuthorizationException.class, () -> sut.update(toUpdate));
        assertEquals("User " + user + " attempted to update a different user's account.", ex.getMessage());
    }

    @Test
    void persistAddsUserRestrictedType() {
        final UserAccount user = generateAccount();
        sut.persist(user);

        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertTrue(result.getTypes().contains(Vocabulary.s_c_omezeny_uzivatel_termitu));
    }

    @Test
    void persistEnsuresAdminTypeIsNotPresentInUserAccount() {
        final UserAccount user = generateAccount();
        user.addType(Vocabulary.s_c_administrator_termitu);
        sut.persist(user);

        final UserAccount result = em.find(UserAccount.class, user.getUri());
        assertFalse(result.getTypes().contains(Vocabulary.s_c_administrator_termitu));
    }

    @Test
    void persistDoesNotGenerateUriIfItIsAlreadyPresent() {
        final UserAccount user = generateAccount();
        final URI originalUri = user.getUri();
        sut.persist(user);

        final UserAccount result = em.find(UserAccount.class, originalUri);
        assertNotNull(result);
        assertEquals(originalUri, result.getUri());
    }
}