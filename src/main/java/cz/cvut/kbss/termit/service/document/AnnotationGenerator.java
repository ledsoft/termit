package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

/**
 * Creates annotations (term occurrences) for vocabulary terms.
 */
@Service
public class AnnotationGenerator {

    // TODO: Switch from DOM to JSoup (it handles HTML better and has some basic support for XML as well)

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationGenerator.class);

    private final TermRepositoryService termService;

    private final TermOccurrenceDao termOccurrenceDao;

    private final SelectorGenerator selectorGenerator;

    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    @Autowired
    public AnnotationGenerator(TermRepositoryService termService, TermOccurrenceDao termOccurrenceDao,
                               SelectorGenerator selectorGenerator) {
        this.termService = termService;
        this.termOccurrenceDao = termOccurrenceDao;
        this.selectorGenerator = selectorGenerator;
    }

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     * <p>
     * The document is annotated using RDFa to indicate term occurrences in the text.
     *
     * @param content    Content of file with identified term occurrences
     * @param source     Source file of the document
     * @param vocabulary Vocabulary whose terms occur in the document
     */
    @Transactional
    public void generateAnnotations(InputStream content, File source, Vocabulary vocabulary) {
        final NodeList rdfaElements = retrieveRDFaElements(content);
        for (int i = 0; i < rdfaElements.getLength(); i++) {
            LOG.trace("Processing RDFa annotated element {}.", rdfaElements.item(i));
            final TermOccurrence occurrence = resolveAnnotation((Element) rdfaElements.item(i), source);
            LOG.trace("Found term occurrence {} in file {}.", occurrence, source);
            termOccurrenceDao.persist(occurrence);
        }
    }

    private NodeList retrieveRDFaElements(InputStream content) {
        try {
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final Document document = builder.parse(content);
            final XPath xPath = xPathFactory.newXPath();
            return (NodeList) xPath.evaluate("//*[@about]", document, XPathConstants.NODESET);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new AnnotationGenerationException("Unable to read RDFa document.", e);
        } catch (XPathExpressionException e) {
            throw new AnnotationGenerationException("Unable to retrieve RDFa annotations from document.", e);
        }
    }

    private TermOccurrence resolveAnnotation(Element rdfaElem, File source) {
        final String termUri = rdfaElem.getAttribute(Constants.RDFa.RESOURCE);
        final Term term = termService.find(URI.create(termUri)).orElseThrow(() -> new NotFoundException(
                "Term with id " + termUri + " denoted by RDFa element " + rdfaElem + " not found."));
        final TermOccurrence occurrence = createOccurrence(term, source);
        final Target target = new Target();
        target.setSource(source);
        target.setSelectors(Collections.singleton(selectorGenerator.createSelector(rdfaElem)));
        occurrence.addTarget(target);
        return occurrence;
    }

    private TermOccurrence createOccurrence(Term term, File source) {
        final TermOccurrence occurrence = new TermOccurrence();
        occurrence.setTerm(term);
        return occurrence;
    }
}
