package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

class UserTest {

    private User sut = new User();

    @Test
    void removeTypeHandlesNullTypesAttribute() {
        sut.removeType(Vocabulary.s_c_administrator_termitu);
    }
}