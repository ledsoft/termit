package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static cz.cvut.kbss.termit.environment.Environment.loadFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

// Needed for the request-scoped occurrence resolver bean to work
@WebAppConfiguration
@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class AnnotationGeneratorTest extends BaseServiceTestRunner {

    private static final URI TERM_ID = URI.create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan");
    private static final URI TERM_TWO_ID = URI
            .create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan-praha");

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao termOccurrenceDao;

    @Autowired
    private Environment environment;

    @Autowired
    private TermDao termDao;

    @Autowired
    private AnnotationGenerator sut;

    private DocumentVocabulary vocabulary;
    private EntityDescriptor vocabDescriptor;
    private cz.cvut.kbss.termit.model.resource.Document document;
    private File file;

    private Term term;
    private Term termTwo;

    @BeforeEach
    void setUp() throws Exception {
        this.term = new Term();
        term.setUri(TERM_ID);
        term.setLabel("Územní plán");
        this.termTwo = new Term();
        termTwo.setUri(TERM_TWO_ID);
        termTwo.setLabel("Územní plán hlavního města Prahy");
        final User author = Generator.generateUserWithId();
        this.vocabulary = new DocumentVocabulary();
        vocabulary.setName("Test Vocabulary");
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        vocabulary.setUri(Generator.generateUri());
        this.document = new cz.cvut.kbss.termit.model.resource.Document();
        document.setAuthor(author);
        document.setDateCreated(new Date());
        document.setName("metropolitan-plan");
        document.setUri(Generator.generateUri());
        document.setVocabulary(vocabulary);
        vocabulary.setDocument(document);
        vocabulary.getGlossary().addTerm(term);
        vocabulary.getGlossary().addTerm(termTwo);
        this.vocabDescriptor = new EntityDescriptor(vocabulary.getUri());
        vocabDescriptor.addAttributeContext(HasProvenanceData.class.getDeclaredField("author"), null);
        vocabulary.setAuthor(author);
        vocabulary.setDateCreated(new Date());
        this.file = new File();
        file.setFileName("rdfa-simple.html");
        document.addFile(file);
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary, vocabDescriptor);
            em.persist(document, vocabDescriptor);
            em.persist(file);
        });
    }

    private void generateFile() throws Exception {
        final java.io.File folder = Files.createTempDirectory("termit").toFile();
        folder.deleteOnExit();
        final String docFolderName = vocabulary.getDocument().getFileDirectoryName();
        final java.io.File docDir = new java.io.File(folder.getAbsolutePath() + java.io.File.separator + docFolderName);
        docDir.mkdirs();
        docDir.deleteOnExit();
        final java.io.File f = new java.io.File(
                folder.getAbsolutePath() + java.io.File.separator + docFolderName + java.io.File.separator +
                        file.getFileName());
        f.createNewFile();
        f.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), folder.getAbsolutePath());
    }

    @Test
    void generateAnnotationsCreatesTermOccurrenceForTermFoundInContentDocument() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        final List<TermOccurrence> result = termOccurrenceDao.findAll(term);
        assertEquals(1, result.size());
    }

    @Test
    void generateAnnotationsSkipsElementsWithUnsupportedType() throws Exception {
        final InputStream content = changeAnnotationType(loadFile("data/rdfa-simple.html"));
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertTrue(termOccurrenceDao.findAll(term).isEmpty());
    }

    private InputStream changeAnnotationType(InputStream content) throws Exception {
        final Document doc = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        final Elements element = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        assert element.size() == 1;
        element.attr(Constants.RDFa.TYPE, cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik);

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnsupportedFileType() {
        final InputStream content = loadFile("data/rdfa-simple.html");
        file.setFileName("test.txt");
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file, document));
        assertThat(ex.getMessage(), containsString("Unsupported type of file"));
    }

    @Test
    void generateAnnotationsResolvesOverlappingAnnotations() throws Exception {
        final InputStream content = loadFile("data/rdfa-overlapping.html");
        file.setFileName("rdfa-overlapping.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        assertEquals(1, termOccurrenceDao.findAll(termTwo).size());
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnknownTermIdentifier() throws Exception {
        final InputStream content = setUnknownTermIdentifier(loadFile("data/rdfa-simple.html"));
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file, document));
        assertThat(ex.getMessage(), containsString("Term with id "));
        assertThat(ex.getMessage(), containsString("not found"));
    }

    private InputStream setUnknownTermIdentifier(InputStream content) throws Exception {
        final Document doc = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        final Elements element = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        assert element.size() == 1;
        element.attr(Constants.RDFa.RESOURCE, Generator.generateUri().toString());

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsHandlesLargerDocumentAnalysis() throws Exception {
        final Term mp = new Term();
        mp.setLabel("Metropolitní plán");
        mp.setUri(URI.create("http://test.org/pojem/metropolitni-plan"));
        final Term ma = new Term();
        ma.setLabel("Správní území Prahy");
        ma.setUri(URI.create("http://test.org/pojem/spravni-uzemi-prahy"));
        final Term area = new Term();
        area.setLabel("Území");
        area.setUri(URI.create("http://test.org/pojem/uzemi"));
        vocabulary.getGlossary().addTerm(mp);
        vocabulary.getGlossary().addTerm(ma);
        vocabulary.getGlossary().addTerm(area);
        transactional(() -> em.merge(vocabulary.getGlossary(), vocabDescriptor));

        final InputStream content = loadFile("data/rdfa-large.html");
        file.setFileName("rdfa-large.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertFalse(termOccurrenceDao.findAll(mp).isEmpty());
        assertFalse(termOccurrenceDao.findAll(ma).isEmpty());
        assertFalse(termOccurrenceDao.findAll(area).isEmpty());
    }

    @Test
    void generateAnnotationsAddsThemSuggestedTypeToIndicateTheyShouldBeVerifiedByUser() throws Exception {
        final InputStream content = loadFile("data/rdfa-overlapping.html");
        file.setFileName("rdfa-overlapping.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        final List<TermOccurrence> result = termOccurrenceDao.findAll();
        result.forEach(to -> assertTrue(
                to.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_vyskyt_termu)));
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        assertEquals(1, termOccurrenceDao.findAll(termTwo).size());
    }

    @Test
    void generateAnnotationsPersistsNewTerms() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms.html");
        file.setFileName("rdfa-new-terms.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file, document);
        final List<Term> resultTerms = termDao.findAll();
        assertEquals(origTerms.size() + 1, resultTerms.size());
        resultTerms.removeAll(origTerms);
        final Term newTerm = resultTerms.get(0);
        assertEquals("město", newTerm.getLabel());
    }

    @Test
    void generateAnnotationsPersistsNewTermsWithTypeSuggestedToIndicateTheyShouldBeVerifiedByUser() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms.html");
        file.setFileName("rdfa-new-terms.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file, document);
        final List<Term> resultTerms = termDao.findAll();
        resultTerms.removeAll(origTerms);
        final Term newTerm = resultTerms.get(0);
        assertTrue(newTerm.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_term));
    }

    @Test
    void generateAnnotationsCreatesTermOccurrencesForNewTerms() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms.html");
        file.setFileName("rdfa-new-terms.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file, document);
        final List<Term> resultTerms = termDao.findAll();
        resultTerms.removeAll(origTerms);
        final Term newTerm = resultTerms.get(0);
        final List<TermOccurrence> result = termOccurrenceDao.findAll(newTerm);
        assertFalse(result.isEmpty());
    }

    @Test
    void generateAnnotationsCreatesNewTermsFromOverlappingAnnotations() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms-overlapping.html");
        file.setFileName("rdfa-new-terms-overlapping.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file, document);
        final List<Term> resultTerms = termDao.findAll();
        resultTerms.removeAll(origTerms);
        assertEquals(1, resultTerms.size());
        final Term newTerm = resultTerms.get(0);
        assertEquals("územní plán hlavní město praha", newTerm.getLabel());
        assertFalse(termOccurrenceDao.findAll(newTerm).isEmpty());
    }

    @Test
    void generateAnnotationsSkipsRDFaAnnotationsWithoutResourceAndContent() throws Exception {
        final InputStream content = removeResourceAndContent(loadFile("data/rdfa-simple.html"));
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertTrue(termOccurrenceDao.findAll().isEmpty());
    }

    private InputStream removeResourceAndContent(InputStream input) throws IOException {
        final Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
        final Elements elements = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        elements.removeAttr(Constants.RDFa.RESOURCE);
        elements.removeAttr(Constants.RDFa.CONTENT);

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsSkipsAnnotationsWithEmptyResource() throws Exception {
        final InputStream content = setEmptyResource(loadFile("data/rdfa-simple.html"));
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertTrue(termOccurrenceDao.findAll().isEmpty());
    }

    private InputStream setEmptyResource(InputStream input) throws IOException {
        final Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
        final Elements elements = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        elements.attr(Constants.RDFa.RESOURCE, "");

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsUsesElementTextContentWhenContentAttributeIsEmptyForNewTerms() throws Exception {
        final InputStream content = setEmptyContentOfNewTerm(loadFile("data/rdfa-new-terms.html"));
        file.setFileName("rdfa-new-terms.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file, document);
        final List<Term> resultTerms = termDao.findAll();
        resultTerms.removeAll(origTerms);
        assertEquals(1, resultTerms.size());
        final Term newTerm = resultTerms.get(0);
        assertEquals("města", newTerm.getLabel());
    }

    private InputStream setEmptyContentOfNewTerm(InputStream input) throws IOException {
        final Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
        final Elements elements = doc.getElementsByAttribute(Constants.RDFa.CONTENT);
        elements.attr(Constants.RDFa.CONTENT, "");

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsSkipsTermOccurrencesWhichAlreadyExistBasedOnSelectors() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        final InputStream contentReloaded = loadFile("data/rdfa-simple.html");
        sut.generateAnnotations(contentReloaded, file, document);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
    }

    @Test
    void generateAnnotationsCreatesTermOccurrenceWhenItHasExistingSelectorButReferencesDifferentTerm()
            throws Exception {
        final Term otherTerm = new Term();
        otherTerm.setUri(Generator.generateUri());
        otherTerm.setLabel("Other term");
        final TermOccurrence to = new TermOccurrence();
        to.setTerm(otherTerm);
        final TextQuoteSelector selector = new TextQuoteSelector("Územní plán");
        selector.setPrefix("RDFa simple");
        selector.setSuffix(" hlavního města Prahy.");
        final Target t = new Target();
        t.setSelectors(Collections.singleton(selector));
        t.setSource(file);
        to.addTarget(t);
        transactional(() -> {
            em.persist(otherTerm);
            em.persist(to);
        });
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file, document);
        final List<TermOccurrence> allOccurrences = termOccurrenceDao.findAllInFile(file);
        assertEquals(2, allOccurrences.size());
        assertTrue(allOccurrences.stream().anyMatch(o -> o.getTerm().equals(otherTerm)));
        assertTrue(allOccurrences.stream().anyMatch(o -> o.getTerm().equals(term)));
    }
}