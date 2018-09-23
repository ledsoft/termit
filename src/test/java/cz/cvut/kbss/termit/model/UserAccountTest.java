package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class UserAccountTest {

    private UserAccount sut;

    @BeforeEach
    void setUp() {
        this.sut = generateAccount();
    }

    @Test
    void toUserReturnsUserWithIdenticalAttributes() {
        final UserAccount ua = generateAccount();
        ua.setTypes(Collections.singleton(Vocabulary.s_c_administrator_termitu));

        final User result = ua.toUser();
        assertAll(() -> assertEquals(ua.getUri(), result.getUri()),
                () -> assertEquals(ua.getFirstName(), result.getFirstName()),
                () -> assertEquals(ua.getLastName(), result.getLastName()),
                () -> assertEquals(ua.getUsername(), result.getUsername()),
                () -> assertEquals(ua.getTypes(), result.getTypes()));
    }

    public static UserAccount generateAccount() {
        final UserAccount ua = new UserAccount();
        ua.setUri(Generator.generateUri());
        ua.setFirstName("first");
        ua.setLastName("last");
        ua.setUsername("username" + Generator.randomInt());
        ua.setPassword("12345");
        return ua;
    }

    @Test
    void erasePasswordRemovesPasswordFromInstance() {
        sut.setPassword("test");
        sut.erasePassword();
        assertNull(sut.getPassword());
    }

    @Test
    void isLockedReturnsFalseForNonLockedInstance() {
        assertFalse(sut.isLocked());
    }

    @Test
    void isLockedReturnsTrueForLockedInstance() {
        sut.addType(Vocabulary.s_c_uzamceny_uzivatel_termitu);
        assertTrue(sut.isLocked());
    }

    @Test
    void lockSetsLockedStatusOfInstance() {
        assertFalse(sut.isLocked());
        sut.lock();
        assertTrue(sut.isLocked());
    }

    @Test
    void unlockRemovesLockedStatusOfInstance() {
        sut.lock();
        assertTrue(sut.isLocked());
        sut.unlock();
        assertFalse(sut.isLocked());
    }

    @Test
    void disableAddsDisabledTypeToInstance() {
        assertTrue(sut.isEnabled());
        sut.disable();
        assertFalse(sut.isEnabled());
    }

    @Test
    void enableRemovesDisabledTypeFromInstance() {
        sut.addType(Vocabulary.s_c_zablokovany_uzivatel_termitu);
        assertFalse(sut.isEnabled());
        sut.enable();
        assertTrue(sut.isEnabled());
    }

    @Test
    void removeTypeHandlesNullTypesAttribute() {
        sut.removeType(Vocabulary.s_c_administrator_termitu);
    }
}