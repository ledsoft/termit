package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRepositoryServiceTest extends BaseServiceTestRunner {

    private static final String EXISTENCE_CHECK_QUERY = "ASK { ?x a ?type . }";

    @Autowired
    private Configuration config;

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceRepositoryService sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findTermsReturnsEmptyListWhenNoTermsAreFoundForResource() {
        final Resource resource = generateResource();

        final List<Term> result = sut.findTags(resource);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private Resource generateResource() {
        final Resource resource = Generator.generateResourceWithId();
        transactional(() -> em.persist(resource));
        return resource;
    }

    @Test
    void findRelatedReturnsEmptyListWhenNoRelatedResourcesAreFoundForResource() {
        final Resource resource = generateResource();

        final List<Resource> result = sut.findRelated(resource);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private Term generateTermWithUriAndPersist() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        transactional(() -> em.persist(t));
        return t;
    }

    @Test
    void setInvalidTagsForValidResource() {
        assertThrows(NotFoundException.class, () -> {
            final Resource resource = generateResource();
            final Set<URI> terms = new HashSet<>();
            terms.add(URI.create("http://unknown.uri/term1"));
            terms.add(URI.create("http://unknown.uri/term2"));
            transactional(() -> sut.setTags(resource, terms));
        });
    }

    @Test
    void addTagsToUntaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource, tags));

        assertEquals(2, sut.findTags(resource).size());
        assertEquals(tags, sut.findTags(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void replaceTagsOfTaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource, tags));

        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        final URI term3 = generateTermWithUriAndPersist().getUri();
        tags2.add(term2);
        tags2.add(term3);
        transactional(() -> sut.setTags(resource, tags2));

        assertEquals(2, sut.findTags(resource).size());
        assertEquals(tags2, sut.findTags(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void setTagsMergesExistingTagsAndNewlySpecifiedTags() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource, tags));
        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        tags2.add(term0);
        tags2.add(term2);

        transactional(() -> sut.setTags(resource, tags2));

        assertEquals(2, sut.findTags(resource).size());
        assertEquals(tags2, sut.findTags(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void persistThrowsValidationExceptionWhenResourceLabelIsMissing() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setLabel(null);
        assertThrows(ValidationException.class, () -> sut.persist(resource));
    }

    @Test
    void removeDeletesTargetAndTermAssignmentsAssociatedWithResource() {
        final Resource resource = generateResource();
        final Term tOne = generateTermWithUriAndPersist();
        final Term tTwo = generateTermWithUriAndPersist();
        final Target target = new Target(resource);
        final TermAssignment assignmentOne = new TermAssignment(tOne, target);
        final TermAssignment assignmentTwo = new TermAssignment(tTwo, target);
        transactional(() -> {
            em.persist(target);
            em.persist(assignmentOne);
            em.persist(assignmentTwo);
        });

        sut.remove(resource);
        assertNull(em.find(Resource.class, resource.getUri()));
        verifyInstancesRemoved(Vocabulary.s_c_prirazeni_termu);
        verifyInstancesRemoved(Vocabulary.s_c_cil);
    }

    private void verifyInstancesRemoved(String type) {
        assertFalse(em.createNativeQuery(EXISTENCE_CHECK_QUERY, Boolean.class).setParameter("type", URI.create(type))
                      .getSingleResult());
    }

    @Test
    void removeDeletesOccurrenceTargetsAndTermOccurrencesAssociatedWithResource() {
        final File file = new File();
        file.setUri(Generator.generateUri());
        file.setLabel("test.txt");
        transactional(() -> em.persist(file));
        final Term tOne = generateTermWithUriAndPersist();
        final OccurrenceTarget target = new OccurrenceTarget(file);
        final TermSelector selector = new TextQuoteSelector("test");
        target.setSelectors(Collections.singleton(selector));
        final TermOccurrence occurrence = new TermOccurrence(tOne, target);
        transactional(() -> {
            em.persist(target);
            em.persist(occurrence);
        });

        sut.remove(file);
        assertNull(em.find(File.class, file.getUri()));
        verifyInstancesRemoved(Vocabulary.s_c_vyskyt_termu);
        verifyInstancesRemoved(Vocabulary.s_c_cil_vyskytu);
        verifyInstancesRemoved(Vocabulary.s_c_selektor_text_quote);
    }

    @Test
    void removeDeletesTermAssignmentsOccurrencesAndAllTargetsAssociatedWithResource() {
        final File file = new File();
        file.setUri(Generator.generateUri());
        file.setLabel("test.txt");
        transactional(() -> em.persist(file));
        final Term tOne = generateTermWithUriAndPersist();
        final OccurrenceTarget occurrenceTarget = new OccurrenceTarget(file);
        final TermSelector selector = new TextQuoteSelector("test");
        occurrenceTarget.setSelectors(Collections.singleton(selector));
        final TermOccurrence occurrence = new TermOccurrence(tOne, occurrenceTarget);
        final Term tTwo = generateTermWithUriAndPersist();
        final Target target = new Target(file);
        final TermAssignment assignmentOne = new TermAssignment(tTwo, target);
        transactional(() -> {
            em.persist(occurrenceTarget);
            em.persist(assignmentOne);
            em.persist(target);
            em.persist(occurrence);
        });

        sut.remove(file);
        verifyInstancesRemoved(Vocabulary.s_c_prirazeni_termu);
        verifyInstancesRemoved(Vocabulary.s_c_cil);
        verifyInstancesRemoved(Vocabulary.s_c_vyskyt_termu);
        verifyInstancesRemoved(Vocabulary.s_c_cil_vyskytu);
        verifyInstancesRemoved(Vocabulary.s_c_selektor_text_quote);
    }

    @Test
    void updateSupportsSubclassesOfResource() {
        final Document doc = new Document();
        doc.setLabel("test document");
        doc.setUri(Generator.generateUri());
        final File fileOne = new File();
        fileOne.setUri(Generator.generateUri());
        fileOne.setLabel("test.txt");
        doc.addFile(fileOne);
        final File fileTwo = new File();
        fileTwo.setUri(Generator.generateUri());
        fileTwo.setLabel("testTwo.html");
        transactional(() -> {
            // Ensure correct RDFS class hierarchy interpretation
            final Repository repository = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repository.getConnection()) {
                final ValueFactory vf = conn.getValueFactory();
                conn.add(vf.createIRI(Vocabulary.s_c_dokument), RDFS.SUBCLASSOF, vf.createIRI(Vocabulary.s_c_zdroj));
            }
            em.persist(doc);
            em.persist(fileOne);
            em.persist(fileTwo);
        });

        final String newName = "Updated name";
        doc.setLabel(newName);
        final String newDescription = "Document description.";
        doc.setDescription(newDescription);
        doc.addFile(fileTwo);
        sut.update(doc);
        final Document result = em.find(Document.class, doc.getUri());
        assertEquals(newName, result.getLabel());
        assertEquals(newDescription, result.getDescription());
        assertEquals(2, result.getFiles().size());
        assertTrue(result.getFiles().contains(fileTwo));
    }

    @Test
    void persistGeneratesResourceIdentifierWhenItIsNotSet() {
        final Resource resource = Generator.generateResource();
        assertNull(resource.getUri());
        transactional(() -> sut.persist(resource));
        assertNotNull(resource.getUri());
        final Resource result = em.find(Resource.class, resource.getUri());
        assertEquals(resource, result);
    }

    @Test
    void generateIdentifierGeneratesIdentifierBasedOnSpecifiedLabel() {
        final String label = "Test resource";
        assertEquals(config.get(ConfigParam.NAMESPACE_RESOURCE) + IdentifierResolver.normalize(label),
                sut.generateIdentifier(label).toString());
    }

    @Test
    void persistThrowsResourceExistsExceptionWhenResourceIdentifierAlreadyExists() {
        final Resource existing = Generator.generateResourceWithId();
        transactional(() -> em.persist(existing));

        final Resource toPersist = Generator.generateResource();
        toPersist.setUri(existing.getUri());
        assertThrows(ResourceExistsException.class, () -> sut.persist(toPersist));
    }
}