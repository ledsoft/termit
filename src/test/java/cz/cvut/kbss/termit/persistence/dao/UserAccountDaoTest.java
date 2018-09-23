package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.UserAccount;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dao")
class UserAccountDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserAccountDao sut;

    @Test
    void findByUsernameReturnsMatchingUser() {
        final UserAccount user = generateAccount();
        transactional(() -> em.persist(user));

        final Optional<UserAccount> result = sut.findByUsername(user.getUsername());
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findByUsernameReturnsEmptyOptionalWhenNoMatchingUserIsFound() {
        final Optional<UserAccount> result = sut.findByUsername("unknown@kbss.felk.cvut.cz");
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void existsByUsernameReturnsTrueForExistingUsername() {
        final UserAccount user = generateAccount();
        transactional(() -> em.persist(user));

        assertTrue(sut.exists(user.getUsername()));
    }

    @Test
    void existsByUsernameReturnsFalseForUnknownUsername() {
        assertFalse(sut.exists("unknownUsername"));
    }
}