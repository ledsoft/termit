package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.util.MetamodelUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VocabularyDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private MetamodelUtils metamodelUtils;

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
            vocab.setDateCreated(new Date());
            return vocab;
        }).collect(Collectors.toList());
        transactional(() -> vocabularies.forEach(v -> em.persist(v, descriptorFor(v))));

        final List<Vocabulary> result = sut.findAll();
        vocabularies.sort(Comparator.comparing(Vocabulary::getName));
        for (int i = 0; i < vocabularies.size(); i++) {
            assertEquals(vocabularies.get(i).getUri(), result.get(i).getUri());
        }
    }

    @Test
    void persistSavesVocabularyIntoContextGivenByItsIri() {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(author);
        vocabulary.setDateCreated(new Date());
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
        vocabulary.setDateCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        final Descriptor descriptor = descriptorFor(vocabulary);
        transactional(() -> em.persist(vocabulary, descriptor));

        final String newName = "Updated vocabulary name";
        vocabulary.setName(newName);
        transactional(() -> sut.update(vocabulary));

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri(), descriptor);
        assertNotNull(result);
        assertEquals(newName, result.getName());
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
        vocabulary.setName(newName);
        transactional(() -> sut.update(vocabulary));
        final List<Vocabulary> result = sut.findAll();
        assertEquals(1, result.size());
        assertEquals(newName, result.get(0).getName());
    }

    @Test
    void updateWorksCorrectlyInContextsForDocumentVocabulary() {
        final DocumentVocabulary vocabulary = new DocumentVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setName("test-vocabulary");
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        final Document doc = new Document();
        doc.setName("test-document");
        doc.setUri(Generator.generateUri());
        final File file = new File();
        file.setName("test-file");
        file.setUri(Generator.generateUri());
        doc.addFile(file);
        vocabulary.setDocument(doc);
        final EntityDescriptor vocabularyDescriptor = new EntityDescriptor(vocabulary.getUri());
        vocabularyDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(Vocabulary.class, "author"),
                new EntityDescriptor(null));
        final EntityDescriptor docDescriptor = new EntityDescriptor(vocabulary.getUri());
        docDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(Document.class, "author"),
                new EntityDescriptor(null));
        final EntityDescriptor fileDescriptor = new EntityDescriptor(vocabulary.getUri());
        fileDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(File.class, "author"),
                new EntityDescriptor(null));
        transactional(() -> {
            em.persist(file, fileDescriptor);
            em.persist(doc, docDescriptor);
            em.persist(vocabulary, vocabularyDescriptor);
        });
        docDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(Document.class, "files"), fileDescriptor);
        vocabularyDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(DocumentVocabulary.class, "document"),
                docDescriptor);

        final String newComment = "New comment";
        vocabulary.setComment(newComment);
        transactional(() -> sut.update(vocabulary));

        final Vocabulary result = em.find(Vocabulary.class, vocabulary.getUri(), vocabularyDescriptor);
        assertEquals(newComment, result.getComment());
    }
}