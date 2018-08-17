package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private User sut = new User();

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