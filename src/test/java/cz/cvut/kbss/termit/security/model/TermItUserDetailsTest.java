package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Set;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermItUserDetailsTest {

    @Test
    void constructorInitializesDefaultUserAuthority() {
        final UserAccount user = generateAccount();
        final TermItUserDetails result = new TermItUserDetails(user);
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void authorityBasedConstructorAddsDefaultAuthority() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final TermItUserDetails result = new TermItUserDetails(generateAccount(), authorities);
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
    }

    @Test
    void constructorResolvesAuthoritiesFromUserTypes() {
        final UserAccount user = generateAccount();
        user.addType(Vocabulary.s_c_administrator_termitu);
        final TermItUserDetails result = new TermItUserDetails(user);
        assertEquals(2, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
    }

    @Test
    void authorityBasedConstructorResolvesAuthoritiesFromUserTypes() {
        final Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_MANAGER"));
        final UserAccount user = generateAccount();
        user.addType(Vocabulary.s_c_administrator_termitu);
        final TermItUserDetails result = new TermItUserDetails(user, authorities);
        assertEquals(3, result.getAuthorities().size());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.USER.getName())));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(UserRole.ADMIN.getName())));
        assertTrue(result.getAuthorities().containsAll(authorities));
    }
}