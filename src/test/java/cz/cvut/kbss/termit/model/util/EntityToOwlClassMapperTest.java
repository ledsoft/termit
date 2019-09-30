package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityToOwlClassMapperTest {

    @Test
    void getOwlClassForEntityReturnsClassIriForEntityClass() {
        final String result = EntityToOwlClassMapper.getOwlClassForEntity(User.class);
        assertEquals(Vocabulary.s_c_uzivatel_termitu, result);
    }

    @Test
    void getOwlClassForEntityThrowsIllegalArgumentForClassNotAnnotatedWithOwlClass() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EntityToOwlClassMapper.getOwlClassForEntity(EntityToOwlClassMapper.class));
        assertEquals("Class " + EntityToOwlClassMapper.class + " is not an OWL entity.", ex.getMessage());
    }
}