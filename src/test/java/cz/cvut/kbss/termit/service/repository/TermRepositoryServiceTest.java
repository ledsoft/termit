package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        this.user = Generator.generateUserAccountWithPassword();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);

        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vrs.persist(vocabulary);
    }

    @Test
    void persistThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> sut.persist(Generator.generateTerm()));
    }

    @Test
    void addTermToVocabularySavesTermAsRoot() {
        final Term term = Generator.generateTermWithId();

        transactional(() -> sut.addRootTermToVocabulary(term, vocabulary));

        transactional(() -> {
            // Need to put in transaction, otherwise EM delegate is closed after find and lazy loading of glossary terms does not work
            final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
            assertNotNull(result);
            assertTrue(result.getGlossary().getRootTerms().contains(term.getUri()));
        });
    }

    @Test
    void addTermToVocabularyGeneratesTermIdentifierWhenItIsNotSet() {
        final Term term = Generator.generateTerm();
        transactional(() -> sut.addRootTermToVocabulary(term, vocabulary));

        assertNotNull(term.getUri());
        final Term result = em.find(Term.class, term.getUri());
        assertNotNull(result);
    }

    @Test
    void addTermToVocabularyAddsTermIntoVocabularyContext() {
        final Term term = Generator.generateTermWithId();

        transactional(() -> sut.addRootTermToVocabulary(term, vocabulary));

        final Term result = em.find(Term.class, term.getUri(), DescriptorFactory.termDescriptor(vocabulary));
        assertEquals(term, result);
    }

    @Test
    void addTermToVocabularyThrowsValidationExceptionWhenTermNameIsBlank() {
        final Term term = Generator.generateTerm();
        term.setUri(Generator.generateUri());
        term.setLabel("");

        final ValidationException exception =
                assertThrows(
                        ValidationException.class, () -> sut.addRootTermToVocabulary(term, vocabulary));
        assertThat(exception.getMessage(), containsString("label must not be blank"));
    }

    @Test
    void addTermToVocabularyThrowsResourceExistsExceptionWhenAnotherTermWithIdenticalAlreadyIriExists() {
        final Term term1 = Generator.generateTerm();
        URI uri = Generator.generateUri();
        term1.setUri(uri);
        sut.addRootTermToVocabulary(term1, vocabulary);

        final Term term2 = Generator.generateTerm();
        term2.setUri(uri);
        assertThrows(
                ResourceExistsException.class, () -> sut.addRootTermToVocabulary(term2, vocabulary));
    }

    @Test
    void addRootTermDoesNotRewriteExistingTermsInGlossary() {
        final Term existing = Generator.generateTermWithId();
        final Term newOne = Generator.generateTermWithId();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(existing);
            em.persist(existing, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
        });

        // Simulate lazily loaded detached root terms
        vocabulary.getGlossary().setRootTerms(null);

        transactional(() -> sut.addRootTermToVocabulary(newOne, vocabulary));

        // Run in transaction to allow lazy fetch of root terms
        transactional(() -> {
            final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri());
            assertNotNull(result);
            assertTrue(result.getRootTerms().contains(existing.getUri()));
            assertTrue(result.getRootTerms().contains(newOne.getUri()));
        });
    }

    @Test
    void generateIdentifierGeneratesTermIdentifierBasedOnVocabularyUriAndTermLabel() {
        final URI vocabularyUri = vocabulary.getUri();
        final String termLabel = "Test term";
        assertEquals(
                vocabularyUri.toString() + Constants.TERM_NAMESPACE_SEPARATOR + "/" +
                        IdentifierResolver.normalize(termLabel),
                sut.generateIdentifier(vocabularyUri, termLabel).toString());
    }

    @Test
    void addChildTermCreatesSubTermForSpecificTerm() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });

        transactional(() -> sut.addChildTerm(child, parent));

        Term result = em.find(Term.class, child.getUri());
        assertNotNull(result);
        assertEquals(parent.getUri(), result.getParent());
    }

    @Test
    void addChildTermGeneratesIdentifierWhenItIsNotSet() {
        final Term parent = Generator.generateTermWithId();
        final Term child = Generator.generateTerm();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent);
            em.merge(vocabulary);
        });

        // This is normally inferred
        parent.setVocabulary(vocabulary.getUri());
        sut.addChildTerm(child, parent);
        assertNotNull(child.getUri());
        final Term result = em.find(Term.class, child.getUri());
        assertNotNull(result);
    }

    @Test
    void addChildTermDoesNotAddTermDirectlyIntoGlossary() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent);
            em.merge(vocabulary.getGlossary());
        });

        sut.addChildTerm(child, parent);
        final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri());
        assertEquals(1, result.getRootTerms().size());
        assertTrue(result.getRootTerms().contains(parent.getUri()));
        assertFalse(result.getRootTerms().contains(child.getUri()));
        assertNotNull(em.find(Term.class, child.getUri()));
    }

    @Test
    void addChildTermPersistsTermIntoVocabularyContext() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent);
            em.merge(vocabulary.getGlossary());
        });

        transactional(() -> sut.addChildTerm(child, parent));
        final Term result = em.find(Term.class, child.getUri(), DescriptorFactory.termDescriptor(vocabulary));
        assertEquals(child, result);
    }

    @Test
    void addChildThrowsResourceExistsExceptionWhenTermWithIdenticalIdentifierAlreadyExists() {
        final Term existing = Generator.generateTermWithId();
        final Term parent = Generator.generateTermWithId();
        final Term child = Generator.generateTerm();
        child.setUri(existing.getUri());
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(existing);
            em.persist(parent);
            em.merge(vocabulary.getGlossary());
        });

        assertThrows(ResourceExistsException.class, () -> sut.addChildTerm(child, parent));
    }

    @Test
    void findAllRootsReturnsRootTermsOnMatchingPage() {
        final List<Term> terms = IntStream.range(0, 10).mapToObj(i -> Generator.generateTermWithId()).collect(
                Collectors.toList());
        vocabulary.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> {
            terms.forEach(em::persist);
            em.merge(vocabulary.getGlossary());
        });

        final List<Term> resultOne = sut.findAllRoots(vocabulary, PageRequest.of(0, 5));
        final List<Term> resultTwo = sut.findAllRoots(vocabulary, PageRequest.of(1, 5));

        assertEquals(5, resultOne.size());
        assertEquals(5, resultTwo.size());

        resultOne.forEach(t -> {
            assertTrue(terms.contains(t));
            assertFalse(resultTwo.contains(t));
        });
        resultTwo.forEach(t -> assertTrue(terms.contains(t)));
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
        toPersist.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        final Descriptor termDescriptor = DescriptorFactory.termDescriptor(vocabulary);
        transactional(() -> {
            terms.forEach(t -> em.persist(t, termDescriptor));
            em.merge(toPersist.getGlossary());
        });

        List<Term> result1 = sut.findAllRoots(vocabulary, "Result");

        assertEquals(5, result1.size());
        result1.forEach(o -> assertTrue(o.getLabel().contains("Result")));
    }

    @Test
    void existsInVocabularyChecksForTermWithMatchingLabel() {
        final Term t = generateTermWithUri();
        transactional(() -> {
            t.setVocabulary(vocabulary.getUri());
            vocabulary.getGlossary().addRootTerm(t);
            em.persist(t, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
        });

        assertTrue(sut.existsInVocabulary(t.getLabel(), vocabulary));
    }

    private Term generateTermWithUri() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        return t;
    }

    @Test
    void updateUpdatesTermWithParent() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addRootTerm(t);
        t.setVocabulary(vocabulary.getUri());
        final Term childOne = generateTermWithUri();
        childOne.setParent(t.getUri());
        childOne.setVocabulary(vocabulary.getUri());
        final Term termTwo = generateTermWithUri();
        vocabulary.getGlossary().addRootTerm(termTwo);
        transactional(() -> {
            em.persist(childOne, DescriptorFactory.termDescriptor(vocabulary));
            em.persist(t, DescriptorFactory.termDescriptor(vocabulary));
            em.persist(termTwo, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });

        childOne.setParent(termTwo.getUri());
        final String newLabel = "new term label";
        childOne.setLabel(newLabel);
        transactional(() -> sut.update(childOne));
        final Term result = em.find(Term.class, childOne.getUri());
        assertEquals(newLabel, result.getLabel());
        assertEquals(termTwo.getUri(), result.getParent());
    }

    @Test
    void updateThrowsValidationExceptionForEmptyTermLabel() {
        final Term t = generateTermWithUri();
        vocabulary.getGlossary().addRootTerm(t);
        vocabulary.getGlossary().addRootTerm(t);
        transactional(() -> {
            em.persist(t);
            em.merge(vocabulary);
        });

        t.setLabel("");
        assertThrows(ValidationException.class, () -> sut.update(t));
    }

    @Test
    void getAssignmentsInfoRetrievesAssignmentData() {
        final Term t = generateTermWithUri();
        t.setVocabulary(vocabulary.getUri());

        final Resource resource = Generator.generateResourceWithId();
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(t.getUri());
        ta.setTarget(new Target(resource));
        transactional(() -> {
            em.persist(t);
            em.persist(resource);
            em.persist(ta.getTarget());
            em.persist(ta);
        });

        final List<TermAssignments> result = sut.getAssignmentsInfo(t);
        assertEquals(1, result.size());
        assertEquals(t.getUri(), result.get(0).getTerm());
        assertEquals(resource.getUri(), result.get(0).getResource());
        assertEquals(resource.getLabel(), result.get(0).getResourceLabel());
    }

    @Test
    void updateRemovesTermFromRootTermsWhenParentIsSetForIt() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        child.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            vocabulary.getGlossary().addRootTerm(child);
            em.persist(parent, DescriptorFactory.termDescriptor(vocabulary));
            em.persist(child, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
        });
        child.setParent(parent.getUri());
        sut.update(child);

        final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri(),
                DescriptorFactory.glossaryDescriptor(vocabulary));
        assertTrue(result.getRootTerms().contains(parent.getUri()));
        assertFalse(result.getRootTerms().contains(child.getUri()));
    }

    @Test
    void updateAddsTermToRootTermsWhenParentIsRemovedFromIt() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        child.setVocabulary(vocabulary.getUri());
        child.setParent(parent.getUri());
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent, DescriptorFactory.termDescriptor(vocabulary));
            em.persist(child, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
        });
        child.setParent(null);
        sut.update(child);

        final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri(),
                DescriptorFactory.glossaryDescriptor(vocabulary));
        assertTrue(result.getRootTerms().contains(parent.getUri()));
        assertTrue(result.getRootTerms().contains(child.getUri()));
    }
}
