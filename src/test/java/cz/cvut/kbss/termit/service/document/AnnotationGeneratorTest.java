package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationGeneratorTest extends BaseServiceTestRunner {

    private static final URI TERM_ID = URI.create("http://onto.fel.cvut.cz/ontologies/mpp/domains/uzemni-plan");

    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

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

    @Disabled
    @Test
    void generateAnnotationsSkipsElementsWithUnsupportedType() throws Exception {
        final InputStream content = changeAnnotationType(loadRDFa("data/rdfa-simple.html"));
        sut.generateAnnotations(content, file, vocabulary);
        assertTrue(termOccurrenceDao.findAll(term).isEmpty());
    }

    private InputStream changeAnnotationType(InputStream content) throws Exception {
        final Document doc = BUILDER_FACTORY.newDocumentBuilder().parse(content);
        final Element element = (Element) XPATH_FACTORY.newXPath().evaluate("//span[@about]", doc, XPathConstants.NODE);
        element.setAttribute(Constants.RDFa.TYPE, cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik);
        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();

        DOMSource source = new DOMSource(doc);
        final StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(source, result);
        return new ByteArrayInputStream(sw.getBuffer().toString().getBytes());
    }
}