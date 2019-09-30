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
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class VocabularyDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private VocabularyDao sut;

    private User author;

    @BeforeEach
    void setUp() {
        this.author = Generator.generateUserWithId();
        transactional(() -> em.persist(author));
        Environment.setCurrentUser(author);
    }

    @Test
    void findAllReturnsVocabulariesOrderedByName() {
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> {
            final Vocabulary vocab = Generator.generateVocabulary();
            vocab.setUri(Generator.generateUri());
            vocab.setAuthor(author);
            vocab.setCreated(new Date());
            return vocab;
        }).collect(Collectors.toList());
        transactional(() -> vocabularies.forEach(v -> em.persist(v, descriptorFor(v))));

        final List<Vocabulary> result = sut.findAll();
        vocabularies.sort(Comparator.comparing(Vocabulary::getLabel));
        for (int i = 0; i < vocabularies.size(); i++) {
            assertEquals(vocabularies.get(i).getUri(), result.get(i).getUri());
        }
    }

    @Test
    void persistSavesVocabularyIntoContextGivenByItsIri() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(author);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        transactional(() -> sut.persist(vocabulary));

        final Descriptor descriptor = descriptorFor(vocabulary);
        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri(), descriptor);
        assertNotNull(result);
    }

    private Descriptor descriptorFor(Vocabulary vocabulary) {
        final Descriptor descriptor = new EntityDescriptor(vocabulary.getUri());
        descriptor.addAttributeDescriptor(
                em.getMetamodel().entity(Vocabulary.class).getAttribute("author").getJavaField(),
                new EntityDescriptor(null));
        return descriptor;
    }

    @Test
    void updateUpdatesVocabularyInContextGivenByItsIri() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(author);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = descriptorFor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));

        final String newName = "Updated vocabulary name";
        vocabulary.setLabel(newName);
        transactional(() -> sut.update(vocabulary));

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri(), descriptor);
        assertNotNull(result);
        assertEquals(newName, result.getLabel());
    }

    @Test
    void updateEvictsPossiblyPreviouslyLoadedInstanceFromSecondLevelCache() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = descriptorFor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));
        // This causes the second level cache to be initialized with the loaded vocabulary (in the default context)
        final List<Vocabulary> vocabularies = sut.findAll();
        assertEquals(1, vocabularies.size());

        final String newName = "Updated vocabulary name";
        vocabulary.setLabel(newName);
        transactional(() -> sut.update(vocabulary));
        final List<Vocabulary> result = sut.findAll();
        assertEquals(1, result.size());
        assertEquals(newName, result.get(0).getLabel());
    }

    @Test
    void updateWorksCorrectlyInContextsForDocumentVocabulary() {
        final DocumentVocabulary vocabulary = new DocumentVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setLabel("test-vocabulary");
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        final Document doc = new Document();
        doc.setLabel("test-document");
        doc.setUri(Generator.generateUri());
        final File file = new File();
        file.setLabel("test-file");
        file.setUri(Generator.generateUri());
        doc.addFile(file);
        vocabulary.setDocument(doc);
        final Descriptor vocabularyDescriptor = DescriptorFactory.vocabularyDescriptor(vocabulary);
        final Descriptor docDescriptor = DescriptorFactory.documentDescriptor(vocabulary);
        transactional(() -> {
            em.persist(file, docDescriptor);
            em.persist(doc, docDescriptor);
            em.persist(vocabulary, vocabularyDescriptor);
        });

        final String newComment = "New comment";
        vocabulary.setComment(newComment);
        transactional(() -> sut.update(vocabulary));

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri(), vocabularyDescriptor);
        assertEquals(newComment, result.getComment());
    }

    @Test
    void updateGlossaryMergesGlossaryIntoPersistenceContext() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = DescriptorFactory.vocabularyDescriptor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));
        final Term term = Generator.generateTermWithId();
        vocabulary.getGlossary().addRootTerm(term);
        final Descriptor termDescriptor = DescriptorFactory.termDescriptor(vocabulary);
        transactional(() -> {
            em.persist(term, termDescriptor);
            sut.updateGlossary(vocabulary);
        });

        transactional(() -> {
            // If we don't run this in transaction, the delegate em is closed right after find and lazy loading of terms
            // does not work
            final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri());
            assertTrue(result.getRootTerms().contains(term.getUri()));
        });
    }

    @Test
    void updateGlossaryMergesGlossaryIntoCorrectRepositoryContext() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = DescriptorFactory.vocabularyDescriptor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));
        final Term term = Generator.generateTermWithId();
        vocabulary.getGlossary().addRootTerm(term);
        final Descriptor termDescriptor = DescriptorFactory.termDescriptor(vocabulary);
        transactional(() -> {
            em.persist(term, termDescriptor);
            sut.updateGlossary(vocabulary);
        });

        final Glossary result = em.find(Glossary.class, vocabulary.getGlossary().getUri(),
                DescriptorFactory.glossaryDescriptor(vocabulary));
        assertTrue(result.getRootTerms().contains(term.getUri()));
    }

    @Test
    void updateGlossaryReturnsManagedGlossaryInstance() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = DescriptorFactory.vocabularyDescriptor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));
        transactional(() -> {
            final Glossary merged = sut.updateGlossary(vocabulary);
            assertTrue(em.contains(merged));
        });
    }

    @Test
    void hasInterVocabularyTermRelationshipsReturnsFalseForVocabulariesWithoutSKOSRelatedTerms() {
        final Vocabulary subjectVocabulary = Generator.generateVocabularyWithId();
        final Vocabulary targetVocabulary = Generator.generateVocabularyWithId();
        transactional(() -> {
            em.persist(subjectVocabulary, DescriptorFactory.vocabularyDescriptor(subjectVocabulary));
            em.persist(targetVocabulary, DescriptorFactory.vocabularyDescriptor(targetVocabulary));
        });

        assertFalse(sut.hasInterVocabularyTermRelationships(subjectVocabulary.getUri(), targetVocabulary.getUri()));
    }

    @Test
    void hasInterVocabularyTermRelationshipsReturnsTrueForSKOSRelatedTermsInSpecifiedVocabularies() {
        final Vocabulary subjectVocabulary = Generator.generateVocabularyWithId();
        final Vocabulary targetVocabulary = Generator.generateVocabularyWithId();
        subjectVocabulary.setImportedVocabularies(Collections.singleton(targetVocabulary.getUri()));
        final Term child = Generator.generateTermWithId();
        final Term parentTerm = Generator.generateTermWithId();
        child.addParentTerm(parentTerm);
        subjectVocabulary.getGlossary().addRootTerm(child);
        child.setVocabulary(subjectVocabulary.getUri());
        targetVocabulary.getGlossary().addRootTerm(parentTerm);
        parentTerm.setVocabulary(targetVocabulary.getUri());
        transactional(() -> {
            em.persist(subjectVocabulary, DescriptorFactory.vocabularyDescriptor(subjectVocabulary));
            em.persist(targetVocabulary, DescriptorFactory.vocabularyDescriptor(targetVocabulary));
            em.persist(child, DescriptorFactory.termDescriptor(child));
            em.persist(parentTerm, DescriptorFactory.termDescriptor(parentTerm));
        });

        assertTrue(sut.hasInterVocabularyTermRelationships(subjectVocabulary.getUri(), targetVocabulary.getUri()));
    }

    @Test
    void hasInterVocabularyTermRelationshipsReturnsTrueForSKOSRelatedTermsInTransitivelyImportedVocabularies() {
        final Vocabulary subjectVocabulary = Generator.generateVocabularyWithId();
        final Vocabulary targetVocabulary = Generator.generateVocabularyWithId();
        final Vocabulary transitiveVocabulary = Generator.generateVocabularyWithId();
        subjectVocabulary.setImportedVocabularies(Collections.singleton(targetVocabulary.getUri()));
        targetVocabulary.setImportedVocabularies(Collections.singleton(transitiveVocabulary.getUri()));
        final Term child = Generator.generateTermWithId();
        final Term parentTerm = Generator.generateTermWithId();
        child.addParentTerm(parentTerm);
        subjectVocabulary.getGlossary().addRootTerm(child);
        child.setVocabulary(subjectVocabulary.getUri());
        transitiveVocabulary.getGlossary().addRootTerm(parentTerm);
        parentTerm.setVocabulary(transitiveVocabulary.getUri());
        transactional(() -> {
            em.persist(subjectVocabulary, DescriptorFactory.vocabularyDescriptor(subjectVocabulary));
            em.persist(targetVocabulary, DescriptorFactory.vocabularyDescriptor(targetVocabulary));
            em.persist(transitiveVocabulary, DescriptorFactory.vocabularyDescriptor(transitiveVocabulary));
            em.persist(child, DescriptorFactory.termDescriptor(child));
            em.persist(parentTerm, DescriptorFactory.termDescriptor(parentTerm));
        });

        assertTrue(sut.hasInterVocabularyTermRelationships(subjectVocabulary.getUri(), targetVocabulary.getUri()));
    }

    @Test
    void getTransitivelyImportedVocabulariesReturnsAllImportedVocabulariesForVocabulary() {
        final Vocabulary subjectVocabulary = Generator.generateVocabularyWithId();
        final Vocabulary importedVocabularyOne = Generator.generateVocabularyWithId();
        final Vocabulary importedVocabularyTwo = Generator.generateVocabularyWithId();
        final Vocabulary transitiveVocabulary = Generator.generateVocabularyWithId();
        subjectVocabulary.setImportedVocabularies(
                new HashSet<>(Arrays.asList(importedVocabularyOne.getUri(), importedVocabularyTwo.getUri())));
        importedVocabularyOne.setImportedVocabularies(Collections.singleton(transitiveVocabulary.getUri()));
        transactional(() -> {
            em.persist(subjectVocabulary, DescriptorFactory.vocabularyDescriptor(subjectVocabulary));
            em.persist(importedVocabularyOne, DescriptorFactory.vocabularyDescriptor(importedVocabularyOne));
            em.persist(importedVocabularyTwo, DescriptorFactory.vocabularyDescriptor(importedVocabularyTwo));
            em.persist(transitiveVocabulary, DescriptorFactory.vocabularyDescriptor(transitiveVocabulary));
        });

        final Collection<URI> result = sut.getTransitivelyImportedVocabularies(subjectVocabulary);
        assertEquals(3, result.size());
        assertTrue(result.contains(importedVocabularyOne.getUri()));
        assertTrue(result.contains(importedVocabularyTwo.getUri()));
        assertTrue(result.contains(transitiveVocabulary.getUri()));
    }
}
