package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsTest {

    @Test
    void constructorInitializesDefaultUserAuthority() {
        final User user = Generator.generateUser();
        final UserDetails result = new UserDetails(user);
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void authorityBasedConstructorAddsDefaultAuthority() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final UserDetails result = new UserDetails(Generator.generateUser(), authorities);
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void constructorResolvesAuthoritiesFromUserTypes() {
        final User user = Generator.generateUser();
        user.addType(Vocabulary.s_c_admin);
        final UserDetails result = new UserDetails(user);
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
    }

    @Test
    void authorityBasedConstructorResolvesAuthoritiesFromUserTypes() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final User user = Generator.generateUser();
        user.addType(Vocabulary.s_c_admin);
        final UserDetails result = new UserDetails(user, authorities);
        assertEquals(3, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
        assertTrue(result.getAuthorities().containsAll(authorities));
    }
}