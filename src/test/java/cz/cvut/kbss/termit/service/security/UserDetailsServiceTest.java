package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserDetailsService sut;

    @Test
    void loadUserByUsernameReturnsUserDetailsForLoadedUser() {
        final User user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        transactional(() -> em.persist(user));

        final UserDetails result = sut.loadUserByUsername(user.getUsername());
        assertNotNull(result);
        assertEquals(user, result.getUser());
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundForUnknownUsername() {
        final String username = "unknownUsername";
        final UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () -> sut.loadUserByUsername(username));
        assertEquals("User with username " + username + " not found.", ex.getMessage());
    }
}