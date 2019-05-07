package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.XPathSelector;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findTermsReturnsTermsWhichAreAssignedToSpecifiedResource() {
        final Resource resource = generateResource();
        final List<Term> terms = generateTerms(resource);

        final List<Term> result = sut.findTerms(resource);
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
    }

    private Resource generateResource() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setAuthor(user);
        resource.setCreated(new Date());
        transactional(() -> em.persist(resource));
        return resource;
    }

    private List<Term> generateTerms(Resource resource) {
        final List<Term> terms = new ArrayList<>();
        final List<Term> matching = new ArrayList<>();
        final List<TermAssignment> assignments = new ArrayList<>();
        final Target target = new Target(resource);
        for (int i = 0; i < Generator.randomInt(2, 10); i++) {
            final Term t = Generator.generateTermWithId();
            terms.add(t);
            if (Generator.randomBoolean() || matching.isEmpty()) {
                matching.add(t);
                final TermAssignment ta = new TermAssignment();
                ta.setTerm(t.getUri());
                ta.setTarget(target);
                assignments.add(ta);
            }
        }
        transactional(() -> {
            terms.forEach(em::persist);
            assignments.forEach(ta -> {
                em.persist(target);
                em.persist(ta);
            });
        });
        return matching;
    }

    @Test
    void findRelatedReturnsResourcesWhichHaveSameTermsAssigned() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> generateResource())
                                                  .collect(Collectors.toList());
        final Resource resource = resources.get(Generator.randomIndex(resources));
        final List<Resource> related = resources.stream().filter(r -> !r.equals(resource) && Generator.randomBoolean())
                                                .collect(
                                                        Collectors.toList());
        generateCommonTerms(resource, related);

        final List<Resource> result = sut.findRelated(resource);
        assertEquals(related.size(), result.size());
        assertTrue(related.containsAll(result));
    }

    private void generateCommonTerms(Resource resource, List<Resource> related) {
        final List<Term> terms = generateTerms(resource);
        final List<TermAssignment> assignments = new ArrayList<>();
        for (Resource res : related) {
            final Term common = terms.get(Generator.randomIndex(terms));
            final TermAssignment ta = new TermAssignment();
            ta.setTerm(common.getUri());
            ta.setTarget(new Target(res));
            assignments.add(ta);
        }
        transactional(() -> assignments.forEach(ta -> {
            em.persist(ta.getTarget());
            em.persist(ta);
        }));
    }

    @Test
    void findAllDoesNotReturnFilesContainedInDocuments() {
        enableRdfsInference(em);
        final Resource rOne = Generator.generateResourceWithId();
        final Document doc = new Document();
        doc.setUri(Generator.generateUri());
        doc.setLabel("document");
        final File file = new File();
        file.setUri(Generator.generateUri());
        file.setLabel("mpp.html");
        doc.addFile(file);
        transactional(() -> {
            em.persist(rOne);
            em.persist(doc);
            em.persist(file);
        });

        final List<Resource> result = sut.findAll();
        assertEquals(2, result.size());
        assertFalse(result.contains(file));
        assertTrue(result.contains(doc));
        final Optional<Resource> docResult = result.stream().filter(r -> r.getUri().equals(doc.getUri())).findAny();
        assertTrue(docResult.isPresent());
        assertTrue(((Document) docResult.get()).getFile(file.getLabel()).isPresent());
    }

    @Test
    void findTermsReturnsDistinctTermsInCaseSomeOccurMultipleTimesInResource() {
        final File resource = new File();
        resource.setLabel("test.html");
        resource.setUri(Generator.generateUri());
        transactional(() -> em.persist(resource));
        final List<Term> terms = generateTerms(resource);
        generateOccurrences(resource, terms);
        final List<Term> result = sut.findTerms(resource);
        final Set<Term> resultSet = new HashSet<>(result);
        assertEquals(result.size(), resultSet.size());
    }

    private void generateOccurrences(File resource, List<Term> terms) {
        final List<TermOccurrence> occurrences = new ArrayList<>();
        for (Term t : terms) {
            final TermOccurrence occurrence = new TermOccurrence(t.getUri(), new OccurrenceTarget(resource));
            // Dummy selector
            occurrence.getTarget().setSelectors(Collections.singleton(new XPathSelector("//div")));
            occurrences.add(occurrence);
        }
        transactional(() -> occurrences.forEach(occ -> {
            em.persist(occ);
            em.persist(occ.getTarget());
        }));
    }

    @Test
    void findAllReturnsResourcesOrderedByLabel() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> generateResource())
                                                  .collect(Collectors.toList());

        final List<Resource> result = sut.findAll();
        resources.sort(Comparator.comparing(Resource::getLabel));
        assertEquals(resources, result);
    }

    @Test
    void persistDocumentWithVocabularyPersistsToVocabularyContext() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Document doc = Generator.generateDocumentWithId();

        transactional(() -> sut.persist(doc, vocabulary));
        final Document result = em
                .find(Document.class, doc.getUri(), DescriptorFactory.documentDescriptor(vocabulary.getUri()));
        assertNotNull(result);
        assertEquals(doc, result);
    }

    @Test
    void persistFileWithVocabularyPersistToVocabularyContext() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final File file = new File();
        file.setLabel("test.html");
        file.setUri(Generator.generateUri());

        transactional(() -> sut.persist(file, vocabulary));
        final File result = em.find(File.class, file.getUri(), DescriptorFactory.fileDescriptor(vocabulary.getUri()));
        assertNotNull(result);
        assertEquals(file, result);
    }

    @Test
    void persistWithVocabularyThrowsIllegalArgumentForGenericResource() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Resource resource = Generator.generateResourceWithId();
        assertThrows(IllegalArgumentException.class, () -> sut.persist(resource, vocabulary));
    }

    @Test
    void updateDocumentWithVocabularyUpdatesDocumentInVocabularyContext() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Document doc = Generator.generateDocumentWithId();

        transactional(() -> em.persist(doc, DescriptorFactory.documentDescriptor(vocabulary)));

        final String newLabel = "new label";
        doc.setLabel(newLabel);

        transactional(() -> sut.update(doc, vocabulary));
        final Document result = em
                .find(Document.class, doc.getUri(), DescriptorFactory.documentDescriptor(vocabulary.getUri()));
        assertNotNull(result);
        assertEquals(newLabel, result.getLabel());
    }

    @Test
    void updateFileWithVocabularyUpdatesFileInVocabularyContext() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final File file = new File();
        file.setLabel("test.html");
        file.setUri(Generator.generateUri());
        transactional(() -> em.persist(file, DescriptorFactory.fileDescriptor(vocabulary)));

        final String newLabel = "new-test.html";
        file.setLabel(newLabel);

        transactional(() -> sut.update(file, vocabulary));
        final File result = em.find(File.class, file.getUri(), DescriptorFactory.fileDescriptor(vocabulary.getUri()));
        assertNotNull(result);
        assertEquals(newLabel, result.getLabel());
    }

    @Test
    void updateWithVocabularyThrowsIllegalArgumentForGenericResource() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Resource resource = Generator.generateResourceWithId();
        transactional(() -> em.persist(resource, DescriptorFactory.fileDescriptor(vocabulary)));

        assertThrows(IllegalArgumentException.class, () -> sut.update(resource, vocabulary));
    }

    @Test
    void updateEvictsCachedVocabularyToPreventIssuesWithStaleReferencesBetweenContexts() {
        final DocumentVocabulary vocabulary = new DocumentVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setLabel("vocabulary");
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        final Document document = Generator.generateDocumentWithId();
        vocabulary.setDocument(document);
        document.setVocabulary(vocabulary.getUri());
        final File file = new File();
        file.setLabel("test.html");
        file.setUri(Generator.generateUri());
        file.setDocument(document);
        document.addFile(file);
        transactional(() -> {
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(document, DescriptorFactory.documentDescriptor(vocabulary));
            em.persist(file, DescriptorFactory.fileDescriptor(vocabulary));
        });

        transactional(() -> {
            final File toRemove = em.getReference(File.class, file.getUri());
            sut.remove(toRemove);
            file.getDocument().removeFile(file);
            sut.update(file.getDocument());
        });

        transactional(() -> {
            final DocumentVocabulary result = em.find(DocumentVocabulary.class, vocabulary.getUri(),
                    DescriptorFactory.vocabularyDescriptor(vocabulary));
            assertThat(result.getDocument().getFiles(), anyOf(nullValue(), empty()));
        });
    }

    @Test
    void detachDetachesInstanceFromPersistenceContext() {
        final Resource resource = Generator.generateResourceWithId();
        transactional(() -> em.persist(resource));

        transactional(() -> {
            final Resource toDetach = sut.find(resource.getUri()).get();
            assertTrue(sut.em.contains(toDetach));
            sut.detach(toDetach);
            assertFalse(sut.em.contains(toDetach));
        });
    }

    @Test
    void detachDoesNothingForNonManagedInstance() {
        final Resource resource = Generator.generateResourceWithId();

        transactional(() -> {
            assertFalse(sut.em.contains(resource));
            sut.detach(resource);
            assertFalse(sut.em.contains(resource));
        });
    }
}