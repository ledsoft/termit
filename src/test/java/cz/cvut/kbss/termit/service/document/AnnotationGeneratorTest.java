package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationGeneratorTest extends BaseServiceTestRunner {

    private static final URI TERM_ID = URI.create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan");

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao termOccurrenceDao;

    @Autowired
    private AnnotationGenerator sut;

    private Vocabulary vocabulary;

    private File file;

    private Term term;

    @BeforeEach
    void setUp() {
        this.term = new Term();
        term.setUri(TERM_ID);
        term.setLabel("Územní plán");
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        this.vocabulary.getGlossary().addTerm(term);
        final User author = Generator.generateUserWithId();
        vocabulary.setAuthor(author);
        vocabulary.setDateCreated(new Date());
        this.file = new File();
        file.setLocation("data/");
        file.setName("rdfa-simple.html");
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary);
            em.persist(file);
        });
    }

    @Disabled
    @Test
    void generateAnnotationsCreatesTermOccurrenceForTermFoundInContentDocument() {
        final InputStream content = loadRDFa("data/rdfa-simple.html");
        sut.generateAnnotations(content, file, vocabulary);
        final List<TermOccurrence> result = termOccurrenceDao.findAll(term);
        assertEquals(1, result.size());
    }

    private static InputStream loadRDFa(String file) {
        return AnnotationGeneratorTest.class.getClassLoader().getResourceAsStream(file);
    }
}