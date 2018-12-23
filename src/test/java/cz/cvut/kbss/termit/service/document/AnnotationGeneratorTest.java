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

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    private String fileLocation;

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
        file.setUri(Generator.generateUri());
        file.setName("rdfa-simple.html");
        file.setDocument(document);
        cz.cvut.kbss.termit.environment.Environment.setCurrentUser(author);
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
                        file.getName());
        f.createNewFile();
        f.deleteOnExit();
        this.fileLocation = f.getAbsolutePath();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), folder.getAbsolutePath());
    }

    @Test
    void generateAnnotationsCreatesTermOccurrenceForTermFoundInContentDocument() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file);
        final List<TermOccurrence> result = termOccurrenceDao.findAll(term);
        assertEquals(1, result.size());
    }

    @Test
    void generateAnnotationsSkipsElementsWithUnsupportedType() throws Exception {
        final InputStream content = changeAnnotationType(loadFile("data/rdfa-simple.html"));
        generateFile();
        sut.generateAnnotations(content, file);
        assertTrue(termOccurrenceDao.findAll(term).isEmpty());
    }

    private InputStream changeAnnotationType(InputStream content) throws Exception {
        final Document doc = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        final Elements element = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        assert element.size() == 1;
        element.attr(Constants.RDFa.TYPE, cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik);

        return new ByteArrayInputStream(doc.toString().getBytes(StandardCharsets.UTF_8.name()));
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnsupportedFileType() {
        final InputStream content = loadFile("data/rdfa-simple.html");
        file.setName("test.txt");
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file));
        assertThat(ex.getMessage(), containsString("Unsupported type of file"));
    }

    @Test
    void generateAnnotationsResolvesOverlappingAnnotations() throws Exception {
        final InputStream content = loadFile("data/rdfa-overlapping.html");
        file.setName("rdfa-overlapping.html");
        generateFile();
        sut.generateAnnotations(content, file);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        assertEquals(1, termOccurrenceDao.findAll(termTwo).size());
    }

    @Test
    void generateAnnotationsThrowsAnnotationGenerationExceptionForUnknownTermIdentifier() throws Exception {
        final InputStream content = setUnknownTermIdentifier(loadFile("data/rdfa-simple.html"));
        final AnnotationGenerationException ex = assertThrows(AnnotationGenerationException.class,
                () -> sut.generateAnnotations(content, file));
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
        transactional(() -> {
            em.persist(mp, new EntityDescriptor(vocabulary.getUri()));
            em.persist(ma, new EntityDescriptor(vocabulary.getUri()));
            em.persist(area, new EntityDescriptor(vocabulary.getUri()));
            em.merge(vocabulary.getGlossary(), vocabDescriptor);
        });

        final InputStream content = loadFile("data/rdfa-large.html");
        file.setName("rdfa-large.html");
        generateFile();
        sut.generateAnnotations(content, file);
        assertFalse(termOccurrenceDao.findAll(mp).isEmpty());
        assertFalse(termOccurrenceDao.findAll(ma).isEmpty());
        assertFalse(termOccurrenceDao.findAll(area).isEmpty());
    }

    @Test
    void generateAnnotationsAddsThemSuggestedTypeToIndicateTheyShouldBeVerifiedByUser() throws Exception {
        final InputStream content = loadFile("data/rdfa-overlapping.html");
        file.setName("rdfa-overlapping.html");
        generateFile();
        sut.generateAnnotations(content, file);
        final List<TermOccurrence> result = termOccurrenceDao.findAll();
        result.forEach(to -> assertTrue(
                to.getTypes().contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_vyskyt_termu)));
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        assertEquals(1, termOccurrenceDao.findAll(termTwo).size());
    }

    @Test
    void generateAnnotationsDoesNotAddTermsForSuggestedKeywords() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms.html");
        file.setName("rdfa-new-terms.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file);
        final List<Term> resultTerms = termDao.findAll();
        assertEquals(origTerms.size(), resultTerms.size());
    }

    @Test
    void generateAnnotationsDoesNotModifyIncomingRdfWhenItContainsNewTermSuggestions() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms.html");
        file.setName("rdfa-new-terms.html");
        generateFile();
        sut.generateAnnotations(content, file);
        final Document originalDoc;
        try (final BufferedReader oldIn = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileLocation)))) {
            final String originalContent = oldIn.lines().collect(Collectors.joining("\n"));
            originalDoc = Jsoup.parse(originalContent);
        }
        try (final BufferedReader newIn = new BufferedReader(
                new InputStreamReader(new FileInputStream(fileLocation)))) {
            final String currentContent = newIn.lines().collect(Collectors.joining("\n"));
            final Document currentDoc = Jsoup.parse(currentContent);
            assertTrue(originalDoc.hasSameValue(currentDoc));
        }
    }

    @Test
    void generateAnnotationsResolvesTermOccurrenceWhenItOverlapsWithNewTermSuggestion() throws Exception {
        final InputStream content = loadFile("data/rdfa-new-terms-overlapping.html");
        file.setName("rdfa-new-terms-overlapping.html");
        generateFile();
        final List<Term> origTerms = termDao.findAll();
        sut.generateAnnotations(content, file);
        final List<Term> resultTerms = termDao.findAll();
        assertEquals(origTerms.size(), resultTerms.size());
        final List<TermOccurrence> to = termOccurrenceDao.findAll(term);
        assertEquals(1, to.size());
    }

    @Test
    void generateAnnotationsSkipsRDFaAnnotationsWithoutResourceAndContent() throws Exception {
        final InputStream content = removeResourceAndContent(loadFile("data/rdfa-simple.html"));
        generateFile();
        sut.generateAnnotations(content, file);
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
        sut.generateAnnotations(content, file);
        assertTrue(termOccurrenceDao.findAll().isEmpty());
    }

    private InputStream setEmptyResource(InputStream input) throws IOException {
        final Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
        final Elements elements = doc.getElementsByAttribute(Constants.RDFa.ABOUT);
        elements.attr(Constants.RDFa.RESOURCE, "");

        return new ByteArrayInputStream(doc.toString().getBytes());
    }

    @Test
    void generateAnnotationsSkipsTermOccurrencesWhichAlreadyExistBasedOnSelectors() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file);
        assertEquals(1, termOccurrenceDao.findAll(term).size());
        final InputStream contentReloaded = loadFile("data/rdfa-simple.html");
        sut.generateAnnotations(contentReloaded, file);
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
        final OccurrenceTarget t = new OccurrenceTarget(file);
        t.setSelectors(Collections.singleton(selector));
        to.setTarget(t);
        transactional(() -> {
            em.persist(t);
            em.persist(otherTerm);
            em.persist(to);
        });
        final InputStream content = loadFile("data/rdfa-simple.html");
        generateFile();
        sut.generateAnnotations(content, file);
        final List<TermOccurrence> allOccurrences = termOccurrenceDao.findAll(file);
        assertEquals(2, allOccurrences.size());
        assertTrue(allOccurrences.stream().anyMatch(o -> o.getTerm().equals(otherTerm)));
        assertTrue(allOccurrences.stream().anyMatch(o -> o.getTerm().equals(term)));
    }
}