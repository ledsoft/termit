package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTermAssignmentsTest {

    @Test
    void constructorAddsSuggestedTypeWhenSuggestedIsTrue() {
        final URI termUri = Generator.generateUri();
        final URI vocabularyUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final ResourceTermAssignments result = new ResourceTermAssignments(termUri, label, vocabularyUri, resourceUri,
                true);
        assertNotNull(result);
        assertTrue(result.getTypes().contains(Vocabulary.s_c_navrzene_prirazeni_termu));
    }
}