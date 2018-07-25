package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private UserRepositoryService sut;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void existsByUsernameReturnsTrueForExistingUsername() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        transactional(() -> em.persist(user));

        assertTrue(sut.exists(user.getUsername()));
    }

    @Test
    void persistEncodesUserPassword() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        final String plainPassword = user.getPassword();

        sut.persist(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsNull() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        user.setPassword(null);
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be empty"));
    }

    @Test
    void persistThrowsValidationExceptionWhenPasswordIsEmpty() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        user.setPassword("");
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.persist(user));
        assertThat(ex.getMessage(), containsString("password must not be empty"));
    }

    @Test
    void updateEncodesPasswordWhenItWasChanged() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
        final String plainPassword = "updatedPassword01";
        user.setPassword(plainPassword);

        sut.update(user);
        final User result = em.find(User.class, user.getUri());
        assertTrue(passwordEncoder.matches(plainPassword, result.getPassword()));
    }

    @Test
    void updateRetainsOriginalPasswordWhenItDoesNotChange() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        final String plainPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));
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
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        final NotFoundException ex = assertThrows(NotFoundException.class, () -> sut.update(user));
        assertEquals("User " + user + " does not exist.", ex.getMessage());
    }

    @Test
    void postLoadErasesPasswordFromInstance() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));

        final Optional<User> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertNull(result.get().getPassword());
    }

    @Test
    void updateThrowsValidationExceptionWhenUpdatedInstanceIsMissingValues() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        transactional(() -> em.persist(user));

        user.setUsername(null);
        user.setPassword(null); // Simulate instance being loaded from repo
        final ValidationException ex = assertThrows(ValidationException.class, () -> sut.update(user));
        assertThat(ex.getMessage(), containsString("username must not be empty"));
    }
}