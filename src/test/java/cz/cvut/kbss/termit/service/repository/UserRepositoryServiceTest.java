package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private UserRepositoryService sut;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void findByUsernameReturnsUserWithMatchingUsername() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        transactional(() -> em.persist(user));

        final Optional<User> result = sut.findByUsername(user.getUsername());
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void persistEncodesUserPassword() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        final String oldPassword = user.getPassword();

        sut.persist(user);
        final Optional<User> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertTrue(passwordEncoder.matches(oldPassword, result.get().getPassword()));
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
}