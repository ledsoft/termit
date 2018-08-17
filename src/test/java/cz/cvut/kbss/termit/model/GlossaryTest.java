package cz.cvut.kbss.termit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlossaryTest {

    @Test
    void addTermAddsTermIntoEmptyGlossary() {
        final Glossary glossary = new Glossary();
        final Term term = new Term();
        glossary.addTerm(term);
        assertNotNull(glossary.getTerms());
        assertTrue(glossary.getTerms().contains(term));
    }
}