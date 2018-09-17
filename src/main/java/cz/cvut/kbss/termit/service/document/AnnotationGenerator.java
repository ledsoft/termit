package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Creates annotations (term occurrences) for vocabulary terms.
 */
@Service
public class AnnotationGenerator {

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     * <p>
     * The document is annotated using RDFa to indicate term occurrences in the text.
     *
     * @param document   Content of document with identified term occurrences
     * @param source     Source file of the document
     * @param vocabulary Vocabulary whose terms occur in the document
     */
    public void generateAnnotations(InputStream document, File source, Vocabulary vocabulary) {
        // TODO
    }
}
