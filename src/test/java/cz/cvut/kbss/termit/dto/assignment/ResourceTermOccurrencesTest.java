package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourceTermOccurrencesTest {

    @Test
    void constructorAddsOccurrenceType() {
        final URI termUri = Generator.generateUri();
        final URI vocabularyUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final int count = 117;
        final ResourceTermOccurrences result = new ResourceTermOccurrences(termUri, label, vocabularyUri, resourceUri,
                count, false);
        assertNotNull(result);
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_vyskyt_termu));
    }

    @Test
    void constructorAddsSuggestedTypeWhenSuggestedIsTrue() {
        final URI termUri = Generator.generateUri();
        final URI vocabularyUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final int count = 117;
        final ResourceTermOccurrences result = new ResourceTermOccurrences(termUri, label, vocabularyUri, resourceUri,
                count, true);
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_vyskyt_termu));
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_navrzeny_vyskyt_termu));
    }
}