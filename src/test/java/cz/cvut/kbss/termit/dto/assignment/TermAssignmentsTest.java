package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TermAssignmentsTest {

    @Test
    void constructorAddsAssignmentType() {
        final URI termUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final TermAssignments result = new TermAssignments(termUri, resourceUri, label, false);
        assertNotNull(result);
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_prirazeni_termu));
    }

    @Test
    void constructorAddsSuggestedTypeWhenSuggestedIsTrue() {
        final URI termUri = Generator.generateUri();
        final URI resourceUri = Generator.generateUri();
        final String label = "Test term";
        final TermAssignments result = new TermAssignments(termUri, resourceUri, label, true);
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_prirazeni_termu));
        assertThat(result.getTypes(), hasItem(Vocabulary.s_c_navrzene_prirazeni_termu));
    }
}