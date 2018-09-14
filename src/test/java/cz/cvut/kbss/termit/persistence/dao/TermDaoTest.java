package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TermDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermDao sut;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setDateCreated(new Date());
        vocabulary.setAuthor(Generator.generateUserWithId());
        transactional(() -> {
            em.persist(vocabulary.getAuthor());
            em.persist(vocabulary);
        });
    }

    @Test
    void findAllWithDefaultPageSpecReturnsAllTerms() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        transactional(() -> em.merge(vocabulary.getGlossary()));

        final List<Term> result = sut.findAll(Constants.DEFAULT_PAGE_SPEC, vocabulary);
        assertEquals(terms.size(), result.size());
        assertEquals(terms, result);
    }

    private List<Term> generateTerms(int count) {
        final List<Term> terms = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Term term = new Term();
            term.setLabel("Test term " + i);
            term.setUri(Generator.generateUri());
            terms.add(term);
        }
        return terms;
    }

    @Test
    void findAllReturnsMatchingPageWithTerms() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        transactional(() -> em.merge(vocabulary.getGlossary()));

        // Paging starts at 0
        final List<Term> result = sut.findAll(PageRequest.of(1, terms.size() / 2), vocabulary);
        final List<Term> subList = terms.subList(terms.size() / 2, terms.size());
        assertEquals(subList, result);
    }

    @Test
    void findAllReturnsOnlyTermsInSpecifiedVocabulary() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        final Vocabulary another = Generator.generateVocabulary();
        another.setUri(Generator.generateUri());
        another.setAuthor(vocabulary.getAuthor());
        another.setDateCreated(new Date());
        another.getGlossary().setTerms(new HashSet<>(generateTerms(4)));
        transactional(() -> {
            em.persist(another);
            em.merge(vocabulary.getGlossary());
        });

        final List<Term> result = sut.findAll(PageRequest.of(0, terms.size() / 2), vocabulary);
        assertEquals(terms.size() / 2, result.size());
        assertTrue(terms.containsAll(result));
    }

    @Test
    void findAllReturnsOnlyRootTerms() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        terms.forEach(t -> t.setSubTerms(new HashSet<>(generateTerms(2))));
        transactional(() -> em.merge(vocabulary.getGlossary()));

        final List<Term> result = sut.findAll(Constants.DEFAULT_PAGE_SPEC, vocabulary);
        assertEquals(terms.size(), result.size());
        for (int i = 0; i < terms.size(); i++) {
            assertEquals(terms.get(i), result.get(i));
            assertEquals(terms.get(i).getSubTerms(), result.get(i).getSubTerms());
        }
    }
}