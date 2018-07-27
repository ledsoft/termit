package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private User user = new User();

    @Test
    void erasePasswordRemovesPasswordFromInstance() {
        user.setPassword("test");
        user.erasePassword();
        assertNull(user.getPassword());
    }

    @Test
    void isLockedReturnsFalseForNonLockedInstance() {
        assertFalse(user.isLocked());
    }

    @Test
    void isLockedReturnsTrueForLockedInstance() {
        user.addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_locked_user);
        assertTrue(user.isLocked());
    }

    @Test
    void lockSetsLockedStatusOfInstance() {
        assertFalse(user.isLocked());
        user.lock();
        assertTrue(user.isLocked());
    }

    @Test
    void unlockRemovesLockedStatusOfInstance() {
        user.lock();
        assertTrue(user.isLocked());
        user.unlock();
        assertFalse(user.isLocked());
    }

    @Test
    void disableAddsDisabledTypeToInstance() {
        assertTrue(user.isEnabled());
        user.disable();
        assertFalse(user.isEnabled());
    }

    @Test
    void enableRemovesDisabledTypeFromInstance() {
        user.addType(Vocabulary.s_c_disabled_user);
        assertFalse(user.isEnabled());
        user.enable();
        assertTrue(user.isEnabled());
    }
}