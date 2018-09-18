package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates annotations (term occurrences) for vocabulary terms.
 */
@Service
public class AnnotationGenerator {

    private final TermRepositoryService termService;

    private final TermOccurrenceDao termOccurrenceDao;

    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    @Autowired
    public AnnotationGenerator(TermRepositoryService termService, TermOccurrenceDao termOccurrenceDao) {
        this.termService = termService;
        this.termOccurrenceDao = termOccurrenceDao;
    }

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     * <p>
     * The document is annotated using RDFa to indicate term occurrences in the text.
     *
     * @param content   Content of file with identified term occurrences
     * @param source     Source file of the document
     * @param vocabulary Vocabulary whose terms occur in the document
     */
    @Transactional
    public void generateAnnotations(InputStream content, File source, Vocabulary vocabulary) {
        try {
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final Document document = builder.parse(content);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private TermOccurrence createOccurrence(Term term) {
        final TermOccurrence occurrence = new TermOccurrence();
        occurrence.setTerm(term);
        return occurrence;
    }
}
