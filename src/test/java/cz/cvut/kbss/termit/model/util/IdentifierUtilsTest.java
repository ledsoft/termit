package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierUtilsTest {

    @Test
    void normalizeTransformsValueToLowerCase() {
        final String value = "CapitalizedSTring";
        assertEquals(value.toLowerCase(), IdentifierUtils.normalize(value));
    }

    @Test
    void normalizeTrimsValue() {
        final String value = "   DDD   ";
        assertEquals(value.trim().toLowerCase(), IdentifierUtils.normalize(value));
    }

    @Test
    void normalizeReplacesSpacesWithDashes() {
        final String value = "Catherine Halsey";
        assertEquals("catherine-halsey", IdentifierUtils.normalize(value));
    }

    @Test
    void normalizeChangesCzechAccutesToAsciiCharacters() {
        final String value = "Strukturální Plán";
        assertEquals("strukturalni-plan", IdentifierUtils.normalize(value));
    }

    @Test
    void normalizeChangesCzechAdornmentsToAsciiCharacters() {
        final String value = "předzahrádka";
        assertEquals("predzahradka", IdentifierUtils.normalize(value));
    }

    @Test
    void generateIdentifierAppendsNormalizedComponentsToBase() {
        final String base = "http://onto.fel.cvut.cz/ontologies/termit/";
        final String compOne = "Catherine";
        final String compTwo = "Halsey";
        final String result = IdentifierUtils.generateIdentifier(base, compOne, compTwo).toString();
        assertEquals(base + "catherine-halsey", result);
    }

    @Test
    void generateIdentifierAppendsSlashWhenBaseDoesNotEndWithIt() {
        final String base = "http://onto.fel.cvut.cz/ontologies/termit";
        final String compOne = "Catherine";
        final String compTwo = "Halsey";
        final String result = IdentifierUtils.generateIdentifier(base, compOne, compTwo).toString();
        assertEquals(base + "/catherine-halsey", result);
    }

    @Test
    void generateIdentifierThrowsIllegalArgumentWhenNoComponentsAreProvided() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> IdentifierUtils.generateIdentifier(Vocabulary.ONTOLOGY_IRI_termit));
        assertEquals("Must provide at least one component for identifier generation.", ex.getMessage());
    }
}