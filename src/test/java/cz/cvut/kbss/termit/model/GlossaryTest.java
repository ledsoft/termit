package cz.cvut.kbss.termit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlossaryTest {

    @Test
    void addTermAddsTermIntoEmptyGlossary() {
        final Glossary glossary = new Glossary();
        final Term term = new Term();
        term.setLabel("test term");
        glossary.addRootTerm(term);
        assertNotNull(glossary.getRootTerms());
        assertTrue(glossary.getRootTerms().contains(term));
    }
}