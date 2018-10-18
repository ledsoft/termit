package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Date;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class VocabularyRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private VocabularyRepositoryService sut;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        this.user = generateAccount();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void persistSetsVocabularyAuthorAndCreationDate() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        sut.persist(vocabulary);

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
        assertNotNull(result);
        assertEquals(user.toUser(), result.getAuthor());
        assertNotNull(result.getDateCreated());
    }

    @Test
    void persistThrowsValidationExceptionWhenVocabularyNameIsBlank() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setName("");
        final ValidationException exception = assertThrows(ValidationException.class, () -> sut.persist(vocabulary));
        assertThat(exception.getMessage(), containsString("name must not be blank"));
    }

    @Test
    void persistGeneratesIdentifierWhenInstanceDoesNotHaveIt() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        sut.persist(vocabulary);
        assertNotNull(vocabulary.getUri());

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
        assertNotNull(result);
        assertThat(result.getUri().toString(), containsString(IdentifierResolver.normalize(vocabulary.getName())));
    }

    @Test
    void persistDoesNotGenerateIdentifierWhenInstanceAlreadyHasOne() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final URI originalUri = vocabulary.getUri();
        sut.persist(vocabulary);
        assertNotNull(vocabulary.getUri());

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
        assertNotNull(result);
        assertEquals(originalUri, result.getUri());
    }

    @Test
    void persistCreatesGlossaryAndModelInstances() {
        final Vocabulary vocabulary = new Vocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setName("TestVocabulary");
        sut.persist(vocabulary);
        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
        assertNotNull(result.getGlossary());
        assertNotNull(result.getModel());
    }

    @Test
    void persistThrowsResourceExistsExceptionWhenAnotherVocabularyWithIdenticalAlreadyIriExists() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setAuthor(user.toUser());
        vocabulary.setDateCreated(new Date());
        transactional(() -> em.persist(vocabulary));

        final Vocabulary toPersist = Generator.generateVocabulary();
        toPersist.setUri(vocabulary.getUri());
        assertThrows(ResourceExistsException.class, () -> sut.persist(toPersist));
    }

    @Test
    void updateThrowsValidationExceptionForEmptyName() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setAuthor(user.toUser());
        vocabulary.setDateCreated(new Date());
        transactional(() -> em.persist(vocabulary));

        vocabulary.setName("");
        assertThrows(ValidationException.class, () -> sut.update(vocabulary));
    }

    private Descriptor descriptorFor(Vocabulary entity) {
        final EntityDescriptor descriptor = new EntityDescriptor(entity.getUri());
        descriptor.addAttributeDescriptor(
                em.getMetamodel().entity(Vocabulary.class).getAttribute("author").getJavaField(),
                new EntityDescriptor(null));
        return descriptor;
    }

    @Test
    void updateSavesUpdatedVocabulary() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setAuthor(user.toUser());
        vocabulary.setDateCreated(new Date());
        transactional(() -> em.persist(vocabulary, descriptorFor(vocabulary)));

        final String newName = "Updated name";
        vocabulary.setName(newName);
        sut.update(vocabulary);
        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri());
        assertNotNull(result);
        assertEquals(newName, result.getName());
    }
}