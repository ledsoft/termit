package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TermItUserDetailsTest {

    @Test
    void constructorInitializesDefaultUserAuthority() {
        final UserAccount user = Generator.generateUserAccount();
        final TermItUserDetails result = new TermItUserDetails(user);
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void authorityBasedConstructorAddsDefaultAuthority() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final TermItUserDetails result = new TermItUserDetails(Generator.generateUserAccount(), authorities);
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void constructorResolvesAuthoritiesFromUserTypes() {
        final UserAccount user = Generator.generateUserAccount();
        user.addType(Vocabulary.s_c_administrator_termitu);
        final TermItUserDetails result = new TermItUserDetails(user);
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
    }

    @Test
    void authorityBasedConstructorResolvesAuthoritiesFromUserTypes() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final UserAccount user = Generator.generateUserAccount();
        user.addType(Vocabulary.s_c_administrator_termitu);
        final TermItUserDetails result = new TermItUserDetails(user, authorities);
        assertEquals(3, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
        assertTrue(result.getAuthorities().containsAll(authorities));
    }

    @Test
    void getUserReturnsCopyOfUser() {
        final UserAccount user = Generator.generateUserAccount();
        final TermItUserDetails sut = new TermItUserDetails(user);
        final UserAccount result = sut.getUser();
        assertEquals(user, result);
        assertNotSame(user, result);
    }
}