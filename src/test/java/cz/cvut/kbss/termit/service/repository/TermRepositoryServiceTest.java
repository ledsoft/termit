package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.*;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class TermRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private VocabularyRepositoryService vrs;

    @Autowired
    private TermRepositoryService sut;

    private UserAccount user;
    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.user = generateAccount();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);

        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vrs.persist(vocabulary);
    }

    @Test
    void persistSetsVocabularyTerm() {
        final Term term = Generator.generateTerm();
        term.setUri(Generator.generateUri());

        sut.addTermToVocabulary(term, vocabulary.getUri());

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());

        assertNotNull(result);
        assertTrue(result.getGlossary().getTerms().contains(term));
    }

    @Test
    void persistThrowsValidationExceptionWhenTermNameIsBlank() {
        final Term term = Generator.generateTerm();
        term.setUri(Generator.generateUri());
        term.setLabel("");

        final ValidationException exception =
                assertThrows(
                        ValidationException.class, () -> sut.addTermToVocabulary(term, vocabulary.getUri()));
        assertThat(exception.getMessage(), containsString("label must not be blank"));
    }

    @Test
    void persistCreatesMultipleTerms() {
        final Term term1 = Generator.generateTerm();
        term1.setUri(Generator.generateUri());

        final Term term2 = Generator.generateTerm();
        term2.setUri(Generator.generateUri());

        sut.addTermToVocabulary(term1, vocabulary.getUri());
        sut.addTermToVocabulary(term2, vocabulary.getUri());

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());

        List<Term> terms = new ArrayList<>();
        terms.add(term1);
        terms.add(term2);

        assertNotNull(result);
        assertTrue(result.getGlossary().getTerms().containsAll(terms));
    }

    @Test
    void persistThrowsResourceExistsExceptionWhenAnotherTermWithIdenticalAlreadyIriExists() {
        final Term term1 = Generator.generateTerm();
        URI uri = Generator.generateUri();
        term1.setUri(uri);
        sut.addTermToVocabulary(term1, vocabulary.getUri());

        final Term term2 = Generator.generateTerm();
        term2.setUri(uri);
        assertThrows(
                ResourceExistsException.class, () -> sut.addTermToVocabulary(term2, vocabulary.getUri()));
    }

    @Test
    void persistCreatesSubTermForSpecificTerm() {
        final Term term1 = Generator.generateTerm();
        URI uri1 = Generator.generateUri();
        term1.setUri(uri1);

        final Term term2 = Generator.generateTerm();
        URI uri2 = Generator.generateUri();
        term2.setUri(uri2);

        sut.addTermToVocabulary(term1, vocabulary.getUri());
        sut.addTermToVocabulary(term2, vocabulary.getUri(), uri1);

        Term result1 = em.find(Term.class, uri1);
        assertNotNull(result1);
        assertTrue(result1.getSubTerms().contains(uri2));
    }

    @Test
    void findTermsWithSpecificLimitAndOffset() {
        Set<Term> terms = new HashSet<>(10);
        Term term;
        for (int i = 0; i < 10; i++) {
            term = generateTermWithUri();
            terms.add(term);
        }

        final Vocabulary toPersist = em.find(Vocabulary.class, vocabulary.getUri());
        toPersist.getGlossary().setTerms(terms);
        vrs.update(toPersist);

        List<Term> result1 = sut.findAll(vocabulary.getUri(), 5, 0);
        List<Term> result2 = sut.findAll(vocabulary.getUri(), 5, 5);

        assertEquals(5, result1.size());
        assertEquals(5, result2.size());

        result1.forEach(o -> assertFalse(result2.contains(o)));
    }

    @Test
    void findTermsBySearchString() {
        Set<Term> terms = new HashSet<>(10);
        Term term;
        for (int i = 0; i < 10; i++) {
            term = Generator.generateTerm();
            term.setUri(Generator.generateUri());
            if (i < 5) {
                term.setLabel("Result " + term.getLabel());
            }
            terms.add(term);
        }

        final Vocabulary toPersist = em.find(Vocabulary.class, vocabulary.getUri());
        toPersist.getGlossary().setTerms(terms);
        vrs.update(toPersist);

        List<Term> result1 = sut.findAll("Result", vocabulary.getUri());

        assertEquals(5, result1.size());
        result1.forEach(o -> assertTrue(o.getLabel().contains("Result")));
    }

    @Test
    void existsInVocabularyChecksForTermWithMatchingLabel() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addTerm(t);
        vrs.update(vocabulary);

        assertTrue(sut.existsInVocabulary(t.getLabel(), vocabulary.getUri()));
    }

    private Term generateTermWithUri() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        return t;
    }

    @Test
    void updateUpdatesTermWithSubTerms() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addTerm(t);
        final Term childOne = generateTermWithUri();
        t.addSubTerm(childOne.getUri());
        final Term termTwo = generateTermWithUri();
        vocabulary.getGlossary().addTerm(t);
        vocabulary.getGlossary().addTerm(termTwo);
        transactional(() -> {
            em.persist(childOne);
            em.merge(vocabulary);
        });

        t.addSubTerm(termTwo.getUri());
        final String newLabel = "new term label";
        t.setLabel(newLabel);
        sut.update(t);
        final Term result = em.find(Term.class, t.getUri());
        assertEquals(newLabel, result.getLabel());
        assertEquals(2, result.getSubTerms().size());
        assertTrue(result.getSubTerms().contains(childOne.getUri()));
        assertTrue(result.getSubTerms().contains(termTwo.getUri()));
    }

    @Test
    void updateThrowsValidationExceptionForEmptyTermLabel() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addTerm(t);
        vocabulary.getGlossary().addTerm(t);
        transactional(() -> em.merge(vocabulary));

        t.setLabel("");
        assertThrows(ValidationException.class, () -> sut.update(t));
    }

    @Test
    void getAssignmentsReturnsTermAssignments() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addTerm(t);

        final Resource resource = Generator.generateResourceWithId();
        resource.setAuthor(user.toUser());
        resource.setDateCreated(new Date());
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(t);
        ta.setTarget(new Target(resource));
        transactional(() -> {
            em.merge(vocabulary);
            em.persist(resource);
            em.persist(ta.getTarget());
            em.persist(ta);
        });

        final List<TermAssignment> result = sut.getAssignments(t);
        assertEquals(1, result.size());
        assertEquals(ta.getTerm(), result.get(0).getTerm());
        assertEquals(ta.getUri(), result.get(0).getUri());
    }
}
