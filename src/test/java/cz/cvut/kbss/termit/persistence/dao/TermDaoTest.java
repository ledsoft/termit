/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.dto.TermInfo;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.util.Constants;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
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
    void findAllRootsWithDefaultPageSpecReturnsAllTerms() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(terms, result);
    }

    private void addTermsAndSave(Collection<Term> terms, Vocabulary vocabulary) {
        vocabulary.getGlossary().setRootTerms(terms.stream().map(Asset::getUri).collect(Collectors.toSet()));
        transactional(() -> {
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
            terms.forEach(t -> {
                t.setVocabulary(vocabulary.getUri());
                em.persist(t, DescriptorFactory.termDescriptor(vocabulary));
            });
        });
    }

    private List<Term> generateTerms(int count) {
        return IntStream.range(0, count).mapToObj(i -> Generator.generateTermWithId())
                        .sorted(Comparator.comparing(Term::getLabel)).collect(Collectors.toList());
    }

    @Test
    void findAllRootsReturnsMatchingPageWithTerms() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        // Paging starts at 0
        final List<Term> result = sut.findAllRoots(vocabulary, PageRequest.of(1, terms.size() / 2));
        final List<Term> subList = terms.subList(terms.size() / 2, terms.size());
        assertEquals(subList, result);
    }

    @Test
    void findAllRootsReturnsOnlyTermsInSpecifiedVocabulary() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);
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
    void findAllRootsReturnsOnlyRootTerms() {
        final List<Term> rootTerms = generateTerms(10);
        addTermsAndSave(new HashSet<>(rootTerms), vocabulary);
        transactional(() -> rootTerms.forEach(t -> {
            final Term child = Generator.generateTermWithId();
            child.setParentTerms(Collections.singleton(t));
            child.setVocabulary(vocabulary.getUri());
            em.persist(child, DescriptorFactory.termDescriptor(vocabulary));
        }));


        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(rootTerms, result);
    }

    @Test
    void findAllRootsBySearchStringReturnsRootTermsWithMatchingLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        final List<Term> result = sut.findAllRoots(terms.get(0).getLabel(), vocabulary);
        assertEquals(1, result.size());
        assertTrue(terms.contains(result.get(0)));
    }

    @Test
    void findAllRootsBySearchStringReturnsRootTermsWhoseDescendantsHaveMatchingLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);
        final Term root = terms.get(Generator.randomIndex(terms));
        final Term child = new Term();
        child.setUri(Generator.generateUri());
        child.setLabel("test");
        child.setParentTerms(Collections.singleton(root));
        final Term matchingDesc = new Term();
        matchingDesc.setUri(Generator.generateUri());
        matchingDesc.setLabel("Metropolitan plan");
        matchingDesc.setParentTerms(Collections.singleton(child));
        transactional(() -> {
            em.persist(child);
            em.persist(matchingDesc);
            em.merge(root);
            insertNarrowerStatements(matchingDesc, child);
        });

        final List<Term> result = sut.findAllRoots("plan", vocabulary);
        assertEquals(1, result.size());
        assertEquals(root, result.get(0));
    }

    /**
     * Simulate the inverse of skos:broader and skos:narrower
     *
     * @param children Terms whose parents need skos:narrower relationships to them
     */
    private void insertNarrowerStatements(Term... children) {
        final Repository repo = em.unwrap(Repository.class);
        final ValueFactory vf = repo.getValueFactory();
        try (final RepositoryConnection conn = repo.getConnection()) {
            conn.begin();
            final IRI narrower = vf.createIRI(SKOS.NARROWER);
            for (Term t : children) {
                for (Term parent : t.getParentTerms()) {
                    conn.add(vf.createStatement(vf.createIRI(parent.getUri().toString()), narrower,
                            vf.createIRI(t.getUri().toString()), vf.createIRI(vocabulary.getUri().toString())));
                }
            }
            conn.commit();
        }
    }

    @Test
    void existsInVocabularyReturnsTrueForLabelExistingInVocabulary() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        final String label = terms.get(0).getLabel();
        assertTrue(sut.existsInVocabulary(label, vocabulary));
    }

    @Test
    void existsInVocabularyReturnsFalseForUnknownLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        assertFalse(sut.existsInVocabulary("unknown label", vocabulary));
    }

    @Test
    void existsInVocabularyReturnsTrueWhenLabelDiffersOnlyInCase() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(terms, vocabulary);

        final String label = terms.get(0).getLabel().toLowerCase();
        assertTrue(sut.existsInVocabulary(label, vocabulary));
    }

    @Test
    void findAllGetsAllTermsInVocabulary() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(terms, vocabulary);

        final List<Term> result = sut.findAll(vocabulary);
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
    }

    @Test
    void findAllReturnsAllTermsFromVocabularyOrderedByLabel() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(terms, vocabulary);

        final List<Term> result = sut.findAll(vocabulary);
        terms.sort(Comparator.comparing(Term::getLabel));
        assertEquals(terms, result);
    }

    @Test
    void persistSavesTermIntoVocabularyContext() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> sut.persist(term));

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

    @Test
    void findAllRootsReturnsOnlyTermsWithMatchingLabelLanguage() {
        final List<Term> terms = generateTerms(5);
        final Term foreignLabelTerm = Generator.generateTermWithId();
        final List<Term> allTerms = new ArrayList<>(terms);
        allTerms.add(foreignLabelTerm);
        addTermsAndSave(allTerms, vocabulary);
        transactional(() -> insertForeignLabel(foreignLabelTerm));

        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(terms, result);
    }

    private void insertForeignLabel(Term term) {
        final Repository repo = em.unwrap(Repository.class);
        try (final RepositoryConnection conn = repo.getConnection()) {
            final ValueFactory vf = conn.getValueFactory();
            conn.remove(vf.createIRI(term.getUri().toString()), RDFS.LABEL, null);
            conn.add(vf.createIRI(term.getUri().toString()), RDFS.LABEL, vf.createLiteral("Adios", "es"));
        }
    }

    @Test
    void findAllReturnsOnlyTermsWithMatchingLanguageLabel() {
        final List<Term> terms = generateTerms(5);
        final Term foreignLabelTerm = Generator.generateTermWithId();
        final List<Term> allTerms = new ArrayList<>(terms);
        allTerms.add(foreignLabelTerm);
        addTermsAndSave(allTerms, vocabulary);
        transactional(() -> insertForeignLabel(foreignLabelTerm));

        final List<Term> result = sut.findAll(vocabulary);
        assertEquals(terms, result);
    }

    @Test
    void findAllRootsIncludingImportsGetsRootTermsFromVocabularyImportChain() {
        final List<Term> directTerms = generateTerms(3);
        addTermsAndSave(directTerms, vocabulary);
        final Vocabulary parent = Generator.generateVocabularyWithId();
        vocabulary.setImportedVocabularies(Collections.singleton(parent.getUri()));
        final Vocabulary grandParent = Generator.generateVocabularyWithId();
        parent.setImportedVocabularies(Collections.singleton(grandParent.getUri()));
        transactional(() -> {
            em.merge(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(parent, DescriptorFactory.vocabularyDescriptor(parent));
            em.persist(grandParent, DescriptorFactory.vocabularyDescriptor(grandParent));
        });
        final List<Term> parentTerms = generateTerms(3);
        addTermsAndSave(parentTerms, parent);
        final List<Term> grandParentTerms = generateTerms(2);
        addTermsAndSave(grandParentTerms, grandParent);
        final List<Term> allTerms = new ArrayList<>(directTerms);
        allTerms.addAll(parentTerms);
        allTerms.addAll(grandParentTerms);
        allTerms.sort(Comparator.comparing(Term::getLabel));

        final List<Term> result = sut.findAllRootsIncludingImports(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(allTerms, result);
    }

    @Test
    void findAllRootsIncludingImportsReturnsVocabularyRootTermsWhenVocabularyDoesNotImportAnyOther() {
        final List<Term> terms = generateTerms(10);
        addTermsAndSave(new HashSet<>(terms), vocabulary);

        final List<Term> result = sut.findAllRootsIncludingImports(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(terms, result);
    }

    @Test
    void findAllRootsIncludingImportsBySearchStringReturnsRootTermsFromVocabularyImportChain() {
        final Vocabulary parent = Generator.generateVocabularyWithId();
        vocabulary.setImportedVocabularies(Collections.singleton(parent.getUri()));
        final Vocabulary grandParent = Generator.generateVocabularyWithId();
        parent.setImportedVocabularies(Collections.singleton(grandParent.getUri()));
        transactional(() -> {
            em.merge(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(parent, DescriptorFactory.vocabularyDescriptor(parent));
            em.persist(grandParent, DescriptorFactory.vocabularyDescriptor(grandParent));
        });
        final List<Term> directTerms = generateTerms(4);
        addTermsAndSave(directTerms, vocabulary);
        final List<Term> parentTerms = generateTerms(3);
        addTermsAndSave(parentTerms, parent);
        final List<Term> grandParentTerms = generateTerms(2);
        addTermsAndSave(grandParentTerms, grandParent);
        transactional(() -> {
            directTerms.get(0).setParentTerms(Collections.singleton(parentTerms.get(0)));
            parentTerms.get(0).setParentTerms(Collections.singleton(grandParentTerms.get(0)));
            directTerms.get(1).setParentTerms(Collections.singleton(parentTerms.get(1)));
            // Parents are in different contexts, so we have to deal with that
            em.merge(directTerms.get(0), DescriptorFactory.termDescriptor(vocabulary)
                                                          .addAttributeDescriptor(Term.getParentTermsField(),
                                                                  DescriptorFactory.vocabularyDescriptor(parent)));
            em.merge(directTerms.get(1), DescriptorFactory.termDescriptor(vocabulary)
                                                          .addAttributeDescriptor(Term.getParentTermsField(),
                                                                  DescriptorFactory.vocabularyDescriptor(parent)));
            em.merge(parentTerms.get(0), DescriptorFactory.termDescriptor(parent)
                                                          .addAttributeDescriptor(Term.getParentTermsField(),
                                                                  DescriptorFactory.vocabularyDescriptor(grandParent)));
            vocabulary.getGlossary().removeRootTerm(directTerms.get(0));
            vocabulary.getGlossary().removeRootTerm(directTerms.get(1));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
            parent.getGlossary().removeRootTerm(parentTerms.get(0));
            em.merge(parent.getGlossary(), DescriptorFactory.glossaryDescriptor(parent));
            insertNarrowerStatements(directTerms.get(0), directTerms.get(1), parentTerms.get(0));
        });

        final String searchString = directTerms.get(0).getLabel()
                                               .substring(0, directTerms.get(0).getLabel().length() - 2);
        final List<Term> result = sut.findAllRootsIncludingImports(searchString, vocabulary);
        assertFalse(result.isEmpty());
        assertThat(result.size(), lessThan(directTerms.size() + parentTerms.size() + grandParentTerms.size()));
        assertTrue(result.contains(grandParentTerms.get(0)));
    }

    @Test
    void persistSupportsReferencingParentTermInSameVocabulary() {
        final Term term = Generator.generateTermWithId();
        final Term parent = Generator.generateTermWithId();
        transactional(() -> {
            parent.setVocabulary(vocabulary.getUri());
            vocabulary.getGlossary().addRootTerm(parent);
            em.persist(parent, DescriptorFactory.termDescriptor(vocabulary));
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
        });
        term.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parent);

        transactional(() -> sut.persist(term));

        final Term result = em.find(Term.class, term.getUri());
        assertNotNull(result);
        assertEquals(Collections.singleton(parent), result.getParentTerms());
    }

    @Test
    void persistSupportsReferencingParentTermInDifferentVocabulary() {
        final Term term = Generator.generateTermWithId();
        final Term parent = Generator.generateTermWithId();
        final Vocabulary parentVoc = Generator.generateVocabularyWithId();
        parent.setVocabulary(parentVoc.getUri());
        transactional(() -> {
            parentVoc.getGlossary().addRootTerm(parent);
            em.persist(parentVoc, DescriptorFactory.vocabularyDescriptor(parentVoc));
            em.persist(parent, DescriptorFactory.termDescriptor(parentVoc));
        });
        term.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parent);

        transactional(() -> sut.persist(term));

        final Term result = em.find(Term.class, term.getUri());
        assertNotNull(result);
        assertEquals(Collections.singleton(parent), result.getParentTerms());
        final TypedQuery<Boolean> query = em.createNativeQuery("ASK {GRAPH ?g {?t ?hasParent ?p .}}", Boolean.class)
                                            .setParameter("g", vocabulary.getUri()).setParameter("t", term.getUri())
                                            .setParameter("hasParent", URI.create(SKOS.BROADER))
                                            .setParameter("p", parent.getUri());
        assertTrue(query.getSingleResult());
    }

    @Test
    void updateSupportsReferencingParentTermInDifferentVocabulary() {
        final Term term = Generator.generateTermWithId();
        final Term parent = Generator.generateTermWithId();
        final Vocabulary parentVoc = Generator.generateVocabularyWithId();
        parent.setVocabulary(parentVoc.getUri());
        term.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parent);
        transactional(() -> {
            parentVoc.getGlossary().addRootTerm(parent);
            em.persist(parentVoc, DescriptorFactory.vocabularyDescriptor(parentVoc));
            em.persist(parent, DescriptorFactory.termDescriptor(parentVoc));
            em.persist(term, DescriptorFactory.termDescriptor(term));
        });

        final Term toUpdate = sut.find(term.getUri()).get();
        assertEquals(Collections.singleton(parent), toUpdate.getParentTerms());
        final String newDefinition = "Updated definition";
        toUpdate.setDefinition(newDefinition);
        transactional(() -> sut.update(toUpdate));

        final Term result = em.find(Term.class, term.getUri());
        assertNotNull(result);
        assertEquals(Collections.singleton(parent), result.getParentTerms());
        assertEquals(newDefinition, result.getDefinition());
    }

    @Test
    void updateSupportsSettingNewParentFromAnotherDifferentVocabulary() {
        final Term term = Generator.generateTermWithId();
        final Term parentOne = Generator.generateTermWithId();
        final Vocabulary parentOneVoc = Generator.generateVocabularyWithId();
        parentOne.setVocabulary(parentOneVoc.getUri());
        final Term parentTwo = Generator.generateTermWithId();
        final Vocabulary parentTwoVoc = Generator.generateVocabularyWithId();
        parentTwo.setVocabulary(parentTwoVoc.getUri());
        term.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parentOne);
        transactional(() -> {
            parentOneVoc.getGlossary().addRootTerm(parentOne);
            em.persist(parentOneVoc, DescriptorFactory.vocabularyDescriptor(parentOneVoc));
            em.persist(parentOne, DescriptorFactory.termDescriptor(parentOneVoc));
            em.persist(term, DescriptorFactory.termDescriptor(term));
            em.persist(parentTwoVoc, DescriptorFactory.vocabularyDescriptor(parentTwoVoc));
            em.persist(parentTwo, DescriptorFactory.termDescriptor(parentTwoVoc));
        });

        final Term toUpdate = sut.find(term.getUri()).get();
        assertEquals(Collections.singleton(parentOne), toUpdate.getParentTerms());
        toUpdate.setParentTerms(Collections.singleton(parentTwo));
        transactional(() -> sut.update(toUpdate));

        final Term result = em.find(Term.class, term.getUri());
        assertNotNull(result);
        assertEquals(Collections.singleton(parentTwo), result.getParentTerms());
    }

    @Test
    void findAllLoadsSubTermsForResults() {
        final Term parent = persistParentWithChild();

        final List<Term> result = sut.findAll(vocabulary);
        assertEquals(2, result.size());
        final Optional<Term> parentResult = result.stream().filter(t -> t.equals(parent)).findFirst();
        assertTrue(parentResult.isPresent());
        assertEquals(parent.getSubTerms(), parentResult.get().getSubTerms());
    }

    private Term persistParentWithChild() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term child = Generator.generateTermWithId();
        child.setVocabulary(vocabulary.getUri());
        child.setParentTerms(Collections.singleton(parent));
        parent.setSubTerms(Collections.singleton(new TermInfo(child)));
        transactional(() -> {
            vocabulary.getGlossary().addRootTerm(parent);
            em.merge(vocabulary.getGlossary(), DescriptorFactory.glossaryDescriptor(vocabulary));
            em.persist(parent, DescriptorFactory.termDescriptor(vocabulary));
            em.persist(child, DescriptorFactory.termDescriptor(vocabulary));
            insertNarrowerStatements(child);
        });
        return parent;
    }

    @Test
    void findAllRootsLoadsSubTermsForResults() {
        final Term parent = persistParentWithChild();
        final List<Term> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(1, result.size());
        assertEquals(parent, result.get(0));
        assertEquals(parent.getSubTerms(), result.get(0).getSubTerms());
    }

    @Test
    void findAllRootsIncludingImportsLoadsSubTermsForResults() {
        final Term parent = persistParentWithChild();
        final List<Term> result = sut.findAllRootsIncludingImports(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        assertEquals(1, result.size());
        assertEquals(parent, result.get(0));
        assertEquals(parent.getSubTerms(), result.get(0).getSubTerms());
    }

    @Test
    void findAllRootsViaSearchStringLoadsSubTermsForResults() {
        final Term parent = persistParentWithChild();
        final String searchString = parent.getSubTerms().iterator().next().getLabel();
        final List<Term> result = sut.findAllRoots(searchString, vocabulary);
        assertEquals(1, result.size());
        assertEquals(parent, result.get(0));
        assertEquals(parent.getSubTerms(), result.get(0).getSubTerms());
    }

    @Test
    void findAllRootsIncludingImportsViaSearchStringLoadsSubTermsForResults() {
        final Term parent = persistParentWithChild();
        final String searchString = parent.getSubTerms().iterator().next().getLabel();
        final List<Term> result = sut.findAllRootsIncludingImports(searchString, vocabulary);
        assertEquals(1, result.size());
        assertEquals(parent, result.get(0));
        assertEquals(parent.getSubTerms(), result.get(0).getSubTerms());
    }

    @Test
    void findLoadsSubTermsForResult() {
        final Term parent = persistParentWithChild();
        final Optional<Term> result = sut.find(parent.getUri());
        assertTrue(result.isPresent());
        assertEquals(parent.getSubTerms(), result.get().getSubTerms());
    }
}
