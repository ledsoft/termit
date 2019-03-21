package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.util.Constants;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        vocabulary.setCreated(new Date());
        vocabulary.setAuthor(Generator.generateUserWithId());
        Environment.setCurrentUser(vocabulary.getAuthor());
        transactional(() -> {
            em.persist(vocabulary.getAuthor());
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });
    }

    @Test
    void findAllWithDefaultPageSpecReturnsAllTerms() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(terms.size(), result.size());
        assertEquals(terms, result);
    }

    private void addTermsAndSave(Set<Term> terms) {
        vocabulary.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> {
            em.merge(vocabulary.getGlossary());
            terms.forEach(t -> {
                t.setVocabulary(vocabulary.getUri());
                em.persist(t);
            });
        });
    }

    private List<Term> generateTerms(int count) {
        return IntStream.range(0, count).mapToObj(i -> Generator.generateTermWithId())
                        .sorted(Comparator.comparing(Term::getLabel)).collect(Collectors.toList());
    }

    @Test
    void findAllReturnsMatchingPageWithTerms() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        // Paging starts at 0
        final List<Term> result = sut.findAllRoots(vocabulary, PageRequest.of(1, terms.size() / 2));
        final List<Term> subList = terms.subList(terms.size() / 2, terms.size());
        assertEquals(subList, result);
    }

    @Test
    void findAllReturnsOnlyTermsInSpecifiedVocabulary() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));
        final Vocabulary another = Generator.generateVocabulary();
        another.setUri(Generator.generateUri());
        another.setAuthor(vocabulary.getAuthor());
        another.setCreated(new Date());
        another.getGlossary().setRootTerms(generateTerms(4).stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> em.persist(another));

        final List<Term> result = sut.findAllRoots(vocabulary, PageRequest.of(0, terms.size() / 2));
        assertEquals(terms.size() / 2, result.size());
        assertTrue(terms.containsAll(result));
    }

    @Test
    void findAllReturnsOnlyRootTerms() {
        final List<Term> terms = generateTerms(10);
        terms.forEach(t -> t.setSubTerms(
                new HashSet<URI>(generateTerms(2).stream().map(Term::getUri).collect(Collectors.toSet()))));
        addTermsAndSave(new HashSet<>(terms));

        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(terms.size(), result.size());
        for (int i = 0; i < terms.size(); i++) {
            assertEquals(terms.get(i), result.get(i));
            assertEquals(terms.get(i).getSubTerms(), result.get(i).getSubTerms());
        }
    }

    @Test
    void findAllBySearchStringReturnsRootTermsWithMatchingLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        final List<Term> result = sut.findAllRoots(terms.get(0).getLabel(), vocabulary);
        assertEquals(1, result.size());
        assertTrue(terms.contains(result.get(0)));
    }

    @Test
    void findAllBySearchStringReturnsRootTermsWhoseDescendantsHaveMatchingLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));
        final Term root = terms.get(Generator.randomIndex(terms));
        final Term child = new Term();
        child.setUri(Generator.generateUri());
        child.setLabel("test");
        root.setSubTerms(Collections.singleton(child.getUri()));
        final Term matchingDesc = new Term();
        matchingDesc.setUri(Generator.generateUri());
        matchingDesc.setLabel("Metropolitan plan");
        child.setSubTerms(Collections.singleton(matchingDesc.getUri()));
        transactional(() -> {
            em.persist(child);
            em.persist(matchingDesc);
            em.merge(root);
        });

        final List<Term> result = sut.findAllRoots("plan", vocabulary);
        assertEquals(1, result.size());
        assertEquals(root, result.get(0));
    }

    @Test
    void existsInVocabularyReturnsTrueForLabelExistingInVocabulary() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        final String label = terms.get(0).getLabel();
        assertTrue(sut.existsInVocabulary(label, vocabulary));
    }

    @Test
    void existsInVocabularyReturnsFalseForUnknownLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        assertFalse(sut.existsInVocabulary("unknown label", vocabulary));
    }

    @Test
    void existsInVocabularyReturnsTrueWhenLabelDiffersOnlyInCase() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms));

        final String label = terms.get(0).getLabel().toLowerCase();
        assertTrue(sut.existsInVocabulary(label, vocabulary));
    }

    @Test
    void findAllGetsAllTermsInVocabulary() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> {
            terms.forEach(em::persist);
            em.merge(vocabulary.getGlossary());
            insertInVocabularyPropertyStatements(terms);
        });

        final List<Term> result = sut.findAll(vocabulary);
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
    }

    /**
     * This method simulates inference of inverse property of the property chain vocabulary->glossary->terms
     */
    private void insertInVocabularyPropertyStatements(List<Term> terms) {
        final Repository repo = em.unwrap(Repository.class);
        final ValueFactory vf = repo.getValueFactory();
        try (final RepositoryConnection conn = repo.getConnection()) {
            conn.begin();
            final IRI vocabIri = vf.createIRI(vocabulary.getUri().toString());
            final IRI inVocabulary = vf.createIRI(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku);
            for (Term t : terms) {
                conn.add(vf.createIRI(t.getUri().toString()), inVocabulary, vocabIri);
            }
            conn.commit();
        }
    }

    @Test
    void findAllReturnsAllTermsFromVocabularyOrderedByLabel() {
        final List<Term> terms = generateTerms(10);
        vocabulary.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> {
            terms.forEach(em::merge);
            em.merge(vocabulary.getGlossary());
            insertInVocabularyPropertyStatements(terms);
        });

        final List<Term> result = sut.findAll(vocabulary);
        terms.sort(Comparator.comparing(Term::getLabel));
        assertEquals(terms, result);
    }

    @Test
    void persistSavesTermIntoVocabularyContext() {
        final Term term = Generator.generateTermWithId();
        transactional(() -> sut.persist(term, vocabulary));

        final Term result = em.find(Term.class, term.getUri(), DescriptorFactory.termDescriptor(vocabulary));
        assertNotNull(result);
        assertEquals(term, result);
    }

    @Test
    void updateUpdatesTermInVocabularyContext() {
        final Term term = Generator.generateTermWithId();
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(term);
            term.setVocabulary(vocabulary.getUri());
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
            em.persist(term, DescriptorFactory.glossaryDescriptor(vocabulary));
        });

        term.setVocabulary(vocabulary.getUri());
        final String updatedLabel = "Updated label";
        final String oldLabel = term.getLabel();
        term.setLabel(updatedLabel);
        transactional(() -> sut.update(term));

        final Term result = em.find(Term.class, term.getUri(), DescriptorFactory.termDescriptor(vocabulary));
        assertEquals(updatedLabel, result.getLabel());
        assertFalse(em.createNativeQuery("ASK WHERE { ?x rdfs:label ?label }", Boolean.class)
                      .setParameter("label", oldLabel, Constants.DEFAULT_LANGUAGE).getSingleResult());
    }
}