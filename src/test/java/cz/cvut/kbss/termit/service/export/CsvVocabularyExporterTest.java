package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CsvVocabularyExporterTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private CsvVocabularyExporter sut;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.vocabulary = Generator.generateVocabulary();
        final User author = Generator.generateUserWithId();
        vocabulary.setAuthor(author);
        vocabulary.setDateCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary);
        });
    }

    @Test
    void exportVocabularyGlossaryOutputsHeaderContainingColumnNamesIntoResult() throws Exception {
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()))) {
            final String header = reader.readLine();
            assertEquals(String.join(",", Term.EXPORT_COLUMNS), header);
        }
    }

    @Test
    void exportVocabularyGlossaryOutputsTermsContainedInVocabularyAsCsv() throws Exception {
        final List<Term> terms = generateTerms();
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()))) {
            final List<String> lines = reader.lines().collect(Collectors.toList());
            // terms + header
            assertEquals(terms.size() + 1, lines.size());
            for (int i = 1; i < lines.size(); i++) {
                final String line = lines.get(i);
                final URI id = URI.create(line.substring(0, line.indexOf(',')));
                assertTrue(terms.stream().anyMatch(t -> t.getUri().equals(id)));
            }
        }
    }

    private List<Term> generateTerms() {
        final List<Term> terms = new ArrayList<>(10);
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final Term term = Generator.generateTermWithId();
            if (Generator.randomBoolean()) {
                term.setSources(Collections.singleton("PSP/c-1/p-2/b-c"));
            }
            terms.add(term);
        }
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        transactional(() -> em.merge(vocabulary));
        return terms;
    }

    @Test
    void exportVocabularyGlossaryExportsTermsOrderedByLabel() throws Exception {
        final List<Term> terms = generateTerms();
        terms.sort(Comparator.comparing(Term::getLabel));
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()))) {
            final List<String> lines = reader.lines().collect(Collectors.toList());
            for (int i = 0; i< terms.size(); i++) {
                final String line = lines.get(i + 1);
                final URI id = URI.create(line.substring(0, line.indexOf(',')));
                assertEquals(terms.get(i).getUri(), id);
            }
        }
    }
}