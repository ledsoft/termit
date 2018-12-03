package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.junit.jupiter.api.Assertions.*;

class TermItUserDetailsServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermItUserDetailsService sut;

    @Test
    void loadUserByUsernameReturnsUserDetailsForLoadedUser() {
        final UserAccount user = generateAccount();
        transactional(() -> em.persist(user));

        final TermItUserDetails result = sut.loadUserByUsername(user.getUsername());
        assertNotNull(result);
        assertEquals(user, result.getUser());
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUsername() {
        final String username = "unknownUsername";
        final UsernameNotFoundException ex =
                assertThrows(UsernameNotFoundException.class, () -> sut.loadUserByUsername(username));
        assertEquals("User with username " + username + " not found.", ex.getMessage());
    }
}