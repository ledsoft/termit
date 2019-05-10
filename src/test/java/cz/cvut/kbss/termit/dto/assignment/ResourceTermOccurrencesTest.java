package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTermOccurrencesTest {

    @Test
    void constructorAddsSuggestedTypeWhenSuggestedIsTrue() {
        final URI termUri = Generator.generateUri();
        final URI vocabularyUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final int count = 117;
        final ResourceTermOccurrences result = new ResourceTermOccurrences(termUri, label, vocabularyUri, resourceUri,
                count, true);
        assertNotNull(result);
        assertTrue(result.getTypes().contains(Vocabulary.s_c_navrzeny_vyskyt_termu));
    }
}