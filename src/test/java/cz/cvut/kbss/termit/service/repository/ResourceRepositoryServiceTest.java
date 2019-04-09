package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.jupiter.api.Assertions.*;

class ResourceRepositoryServiceTest extends BaseServiceTestRunner {

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
        verifyInstancesRemoved(Vocabulary.s_c_prirazeni_termu, em);
        verifyInstancesRemoved(Vocabulary.s_c_cil, em);
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
        verifyInstancesRemoved(Vocabulary.s_c_vyskyt_termu, em);
        verifyInstancesRemoved(Vocabulary.s_c_cil_vyskytu, em);
        verifyInstancesRemoved(Vocabulary.s_c_selektor_text_quote, em);
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
        verifyInstancesRemoved(Vocabulary.s_c_prirazeni_termu, em);
        verifyInstancesRemoved(Vocabulary.s_c_cil, em);
        verifyInstancesRemoved(Vocabulary.s_c_vyskyt_termu, em);
        verifyInstancesRemoved(Vocabulary.s_c_cil_vyskytu, em);
        verifyInstancesRemoved(Vocabulary.s_c_selektor_text_quote, em);
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

    @Test
    void removeDeletesReferenceFromParentDocumentToRemovedFile() {
        final File file = new File();
        file.setUri(Generator.generateUri());
        file.setLabel("test.txt");
        final Document parent = new Document();
        parent.setUri(Generator.generateUri());
        parent.setLabel("Parent document");
        parent.addFile(file);
        file.setDocument(parent);   // Manually set the inferred attribute
        transactional(() -> {
            em.persist(file);
            em.persist(parent);
        });

        transactional(() -> sut.remove(file));

        assertFalse(sut.exists(file.getUri()));
        final Document result = em.find(Document.class, parent.getUri());
        assertThat(result.getFiles(), anyOf(nullValue(), empty()));
    }

    @Test
    void findAssignmentsReturnsAssignmentsRelatedToSpecifiedResource() {
        final Resource resource = Generator.generateResourceWithId();
        final Target target = new Target(resource);
        final Term term = Generator.generateTermWithId();
        final TermAssignment ta = new TermAssignment(term, target);
        transactional(() -> {
            em.persist(target);
            em.persist(resource);
            em.persist(term);
            em.persist(ta);
        });

        final List<TermAssignment> result = sut.findAssignments(resource);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ta.getUri(), result.get(0).getUri());
    }
}