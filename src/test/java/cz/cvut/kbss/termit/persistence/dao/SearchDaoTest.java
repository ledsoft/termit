package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.LabelSearchResult;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SearchDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private SearchDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
    }

    @Test
    void searchByLabelFindsTermsWithMatchingLabel() {
        final Vocabulary vocabulary = generateTerms();
        transactional(() -> {
            em.persist(vocabulary);
            insertInVocabularyStatements(vocabulary);
        });
        final Collection<Term> matching = vocabulary.getGlossary().getTerms().stream()
                                                    .filter(t -> t.getLabel().contains("Matching")).collect(
                        Collectors.toList());

        final List<LabelSearchResult> result = sut.searchByLabel("matching");
        assertEquals(matching.size(), result.size());
        for (LabelSearchResult item : result) {
            assertTrue(item.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_term));
            assertTrue(matching.stream().anyMatch(t -> t.getUri().equals(item.getUri())));
        }
    }

    private Vocabulary generateTerms() {
        final Vocabulary vocabulary = new Vocabulary();
        vocabulary.setName("test");
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setAuthor(user);
        vocabulary.setDateCreated(new Date());
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final Term term = new Term();
            term.setUri(Generator.generateUri());
            term.setLabel(Generator.randomBoolean() ? "Matching label " + i : "Unknown label " + i);
            vocabulary.getGlossary().addTerm(term);
        }
        return vocabulary;
    }

    private void insertInVocabularyStatements(Vocabulary vocabulary) {
        // This normally happens by using SPIN rules on repository level
        final Repository repo = em.unwrap(Repository.class);
        try (final RepositoryConnection con = repo.getConnection()) {
            final ValueFactory vf = con.getValueFactory();
            vocabulary.getGlossary().getTerms().forEach(t -> con.add(vf.createIRI(t.getUri().toString()),
                    vf.createIRI(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku),
                    vf.createIRI(vocabulary.getUri().toString())));
        }
    }

    @Test
    void searchByLabelFindsVocabulariesWithMatchingLabel() {
        final List<Vocabulary> vocabularies = generateVocabularies();
        transactional(() -> vocabularies.forEach(em::persist));
        final Collection<Vocabulary> matching = vocabularies.stream().filter(v -> v.getName().contains("Matching"))
                                                            .collect(Collectors.toList());
        final List<LabelSearchResult> result = sut.searchByLabel("matching");
        assertEquals(matching.size(), result.size());
        for (LabelSearchResult item : result) {
            assertTrue(item.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik));
            assertTrue(matching.stream().anyMatch(t -> t.getUri().equals(item.getUri())));
        }
    }

    private List<Vocabulary> generateVocabularies() {
        final List<Vocabulary> vocabularies = IntStream.range(0, 10).mapToObj(i -> Generator.generateVocabulary())
                                                       .collect(
                                                               Collectors.toList());
        vocabularies.forEach(v -> {
            v.setUri(Generator.generateUri());
            v.setAuthor(user);
            v.setDateCreated(new Date());
            if (Generator.randomBoolean()) {
                v.setName("Matching label " + Generator.randomInt());
            }
        });
        return vocabularies;
    }

    @Test
    void searchByLabelFindsVocabulariesAndTermsWithMatchingLabel() {
        final Vocabulary terms = generateTerms();
        final List<Vocabulary> vocabularies = generateVocabularies();
        transactional(() -> {
            em.persist(terms);
            insertInVocabularyStatements(terms);
            vocabularies.forEach(em::persist);
        });
        final Collection<Term> matchingTerms = terms.getGlossary().getTerms().stream()
                                                    .filter(t -> t.getLabel().contains("Matching")).collect(
                        Collectors.toList());
        final Collection<Vocabulary> matchingVocabularies = vocabularies.stream()
                                                                        .filter(v -> v.getName().contains("Matching"))
                                                                        .collect(Collectors.toList());
        final List<LabelSearchResult> result = sut.searchByLabel("matching");
        assertEquals(matchingTerms.size() + matchingVocabularies.size(), result.size());
        for (LabelSearchResult item : result) {
            if (item.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_term)) {
                assertTrue(matchingTerms.stream().anyMatch(t -> t.getUri().equals(item.getUri())));
            } else {
                assertTrue(matchingVocabularies.stream().anyMatch(v -> v.getUri().equals(item.getUri())));
            }
        }
    }
}