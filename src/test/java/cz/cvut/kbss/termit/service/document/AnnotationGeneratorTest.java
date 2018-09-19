package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

// Needed for the request-scoped occurrence resolver bean to work
@WebAppConfiguration
class AnnotationGeneratorTest extends BaseServiceTestRunner {

    private static final URI TERM_ID = URI.create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan");
    private static final URI TERM_TWO_ID = URI
            .create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan-praha");

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao termOccurrenceDao;

    @Autowired
    private AnnotationGenerator sut;

    private Vocabulary vocabulary;

    private File file;

    private Term term;
    private Term termTwo;

    @BeforeEach
    void setUp() {
        this.term = new Term();
        term.setUri(TERM_ID);
        term.setLabel("Územní plán");
        this.termTwo = new Term();
        termTwo.setUri(TERM_TWO_ID);
        termTwo.setLabel("Územní plán hlavního města Prahy");
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        this.vocabulary.getGlossary().addTerm(term);
        this.vocabulary.getGlossary().addTerm(termTwo);
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

    @Test
    void generateAnnotationsSkipsElementsWithUnsupportedType() throws Exception {
        final InputStream content = changeAnnotationType(loadRDFa("data/rdfa-simple.html"));
        sut.generateAnnotations(content, file, vocabulary);
        assertTrue(termOccurrenceDao.findAll(term).isEmpty());
    }

    private InputStream changeAnnotationType(InputStream content) throws Exception {
        final Document doc = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        final Elements element = doc.getElementsByAttribute("about");
        assert element.size() == 1;
        element.attr(Constants.RDFa.TYPE, cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik);

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsResolvesPrefixes() {
        final InputStream content = loadRDFa("data/rdfa-simple.html");
        sut.generateAnnotations(content, file, vocabulary);
        final List<TermOccurrence> result = termOccurrenceDao.findAll(term);
        assertEquals(1, result.size());
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnsupportedFileType() {
        final InputStream content = loadRDFa("data/rdfa-simple.html");
        file.setName("test.txt");
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file, vocabulary));
        assertThat(ex.getMessage(), containsString("Unsupported type of file"));
    }

    @Test
    void generateAnnotationsResolvesOverlappingAnnotations() {
        final InputStream content = loadRDFa("data/rdfa-overlapping.html");
        sut.generateAnnotations(content, file, vocabulary);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        assertEquals(1, termOccurrenceDao.findAll(termTwo).size());
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnknownTermIdentifier() throws Exception {
        final InputStream content = setUnknownTermIdentifier(loadRDFa("data/rdfa-simple.html"));
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file, vocabulary));
        assertThat(ex.getMessage(), containsString("Term with id "));
        assertThat(ex.getMessage(), containsString("not found"));
    }

    private InputStream setUnknownTermIdentifier(InputStream content) throws Exception {
        final Document doc = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        final Elements element = doc.getElementsByAttribute("about");
        assert element.size() == 1;
        element.attr(Constants.RDFa.RESOURCE, Generator.generateUri().toString());

        return new ByteArrayInputStream(doc.toString().getBytes());
    }
}