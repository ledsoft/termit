package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dao")
class UserDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserDao sut;

    @Test
    void findByUsernameReturnsMatchingUser() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        transactional(() -> em.persist(user));

        final Optional<User> result = sut.findByUsername(user.getUsername());
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findByUsernameReturnsEmptyOptionalWhenNoMatchingUserIsFound() {
        final Optional<User> result = sut.findByUsername("unknown@kbss.felk.cvut.cz");
        assertNotNull(result);
        assertFalse(result.isPresent());
    }
}