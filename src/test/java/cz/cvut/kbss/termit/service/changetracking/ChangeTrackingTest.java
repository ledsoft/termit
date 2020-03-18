package cz.cvut.kbss.termit.service.changetracking;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.PersistChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.UpdateChangeRecord;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.persistence.dao.changetracking.ChangeRecordDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChangeTrackingTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ChangeRecordDao changeRecordDao;

    @Autowired
    private VocabularyRepositoryService vocabularyService;

    @Autowired
    private TermRepositoryService termService;

    private User author;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        this.vocabulary = Generator.generateVocabularyWithId();
        transactional(() -> em.persist(author));
    }

    @Test
    void persistingVocabularyCreatesCreationChangeRecord() {
        enableRdfsInference(em);
        transactional(() -> vocabularyService.persist(vocabulary));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(vocabulary);
        assertEquals(1, result.size());
        assertEquals(vocabulary.getUri(), result.get(0).getChangedEntity());
        assertThat(result.get(0), instanceOf(PersistChangeRecord.class));
    }

    @Test
    void persistingTermCreatesCreationChangeRecord() {
        enableRdfsInference(em);
        transactional(() -> em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary)));
        final Term term = Generator.generateTermWithId();
        transactional(() -> termService.addRootTermToVocabulary(term, vocabulary));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(term);
        assertEquals(1, result.size());
        assertEquals(term.getUri(), result.get(0).getChangedEntity());
        assertThat(result.get(0), instanceOf(PersistChangeRecord.class));
    }

    @Test
    void updatingVocabularyLiteralAttributeCreatesUpdateChangeRecord() {
        enableRdfsInference(em);
        transactional(() -> em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary)));
        vocabulary.setLabel("Updated vocabulary label");
        transactional(() -> vocabularyService.update(vocabulary));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(vocabulary);
        assertEquals(1, result.size());
        assertEquals(vocabulary.getUri(), result.get(0).getChangedEntity());
        assertThat(result.get(0), instanceOf(UpdateChangeRecord.class));
        assertEquals(RDFS.LABEL, ((UpdateChangeRecord) result.get(0)).getChangedAttribute().toString());
    }

    @Test
    void updatingVocabularyReferenceAndLiteralAttributesCreatesTwoUpdateRecords() {
        enableRdfsInference(em);
        final Vocabulary imported = Generator.generateVocabularyWithId();
        transactional(() -> {
            em.persist(imported, DescriptorFactory.vocabularyDescriptor(imported));
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });
        vocabulary.setLabel("Updated vocabulary label");
        vocabulary.setImportedVocabularies(Collections.singleton(imported.getUri()));
        transactional(() -> vocabularyService.update(vocabulary));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(vocabulary);
        assertEquals(2, result.size());
        result.forEach(chr -> {
            assertEquals(vocabulary.getUri(), chr.getChangedEntity());
            assertThat(result.get(0), instanceOf(UpdateChangeRecord.class));
            assertThat(((UpdateChangeRecord) chr).getChangedAttribute().toString(), anyOf(equalTo(RDFS.LABEL),
                    equalTo(cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik)));
        });
    }

    @Test
    void updatingTermLiteralAttributeCreatesChangeRecord() {
        enableRdfsInference(em);
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(term, DescriptorFactory.termDescriptor(term));
        });
        term.setDefinition("Updated term definition.");
        transactional(() -> termService.update(term));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(term);
        assertEquals(1, result.size());
        assertEquals(term.getUri(), result.get(0).getChangedEntity());
        assertThat(result.get(0), instanceOf(UpdateChangeRecord.class));
        assertEquals(SKOS.DEFINITION, ((UpdateChangeRecord) result.get(0)).getChangedAttribute().toString());
    }

    @Test
    void updatingTermReferenceAttributeCreatesChangeRecord() {
        enableRdfsInference(em);
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(parent, DescriptorFactory.termDescriptor(parent));
            em.persist(term, DescriptorFactory.termDescriptor(term));
        });
        term.addParentTerm(parent);
        transactional(() -> termService.update(term));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(term);
        assertEquals(1, result.size());
        assertEquals(term.getUri(), result.get(0).getChangedEntity());
        assertThat(result.get(0), instanceOf(UpdateChangeRecord.class));
        assertEquals(SKOS.BROADER, ((UpdateChangeRecord) result.get(0)).getChangedAttribute().toString());
    }

    @Test
    void updatingTermLiteralAttributesCreatesChangeRecordWithOriginalAndNewValue() {
        enableRdfsInference(em);
        final Term term = Generator.generateTermWithId();
        final String originalDefinition = term.getDefinition();
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(term, DescriptorFactory.termDescriptor(term));
        });
        final String newDefinition = "Updated term definition.";
        term.setDefinition(newDefinition);
        transactional(() -> termService.update(term));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(term);
        assertEquals(1, result.size());
        assertEquals(Collections.singleton(originalDefinition),
                ((UpdateChangeRecord) result.get(0)).getOriginalValue());
        assertEquals(Collections.singleton(newDefinition), ((UpdateChangeRecord) result.get(0)).getNewValue());
    }

    @Test
    void updatingTermReferenceAttributeCreatesChangeRecordWithOriginalAndNewValue() {
        enableRdfsInference(em);
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        transactional(() -> {
            em.persist(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(parent, DescriptorFactory.termDescriptor(parent));
            em.persist(term, DescriptorFactory.termDescriptor(term));
        });
        term.addParentTerm(parent);
        transactional(() -> termService.update(term));

        final List<AbstractChangeRecord> result = changeRecordDao.findAll(term);
        assertNull(((UpdateChangeRecord) result.get(0)).getOriginalValue());
        assertEquals(Collections.singleton(parent.getUri()), ((UpdateChangeRecord) result.get(0)).getNewValue());
    }
}
