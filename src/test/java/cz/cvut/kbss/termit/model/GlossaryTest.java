package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.environment.Generator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlossaryTest {

    @Test
    void addTermAddsTermIntoEmptyGlossary() {
        final Glossary glossary = new Glossary();
        final Term term = Generator.generateTermWithId();
        glossary.addRootTerm(term);
        assertNotNull(glossary.getRootTerms());
        assertTrue(glossary.getRootTerms().contains(term.getUri()));
    }
}