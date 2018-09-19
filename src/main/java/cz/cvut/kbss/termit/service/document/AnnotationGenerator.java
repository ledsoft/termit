package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * Creates annotations (term occurrences) for vocabulary terms.
 * <p>
 * The generated {@link TermOccurrence}s are assigned a special type so that it is clear they have been suggested by an
 * automated procedure and should be reviewed.
 */
@Service
public class AnnotationGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationGenerator.class);

    private final TermOccurrenceDao termOccurrenceDao;

    private final TermOccurrenceResolver htmlOccurrenceResolver;

    @Autowired
    public AnnotationGenerator(TermOccurrenceDao termOccurrenceDao,
                               @Qualifier("html") TermOccurrenceResolver htmlOccurrenceResolver) {
        this.termOccurrenceDao = termOccurrenceDao;
        this.htmlOccurrenceResolver = htmlOccurrenceResolver;
    }

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     *
     * @param content    Content of file with identified term occurrences
     * @param source     Source file of the annotated document
     * @param vocabulary Vocabulary whose terms occur in the document
     */
    @Transactional
    public void generateAnnotations(InputStream content, File source, Vocabulary vocabulary) {
        // This will allow us to potentially support different types of files
        if (htmlOccurrenceResolver.supports(source)) {
            LOG.debug("Resolving annotations of HTML file {}.", source);
            final List<TermOccurrence> occurrences = htmlOccurrenceResolver.findTermOccurrences(content, source);
            occurrences.forEach(o -> {
                o.addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_vyskyt_termu);
                termOccurrenceDao.persist(o);
            });
        } else {
            throw new AnnotationGenerationException("Unsupported type of file " + source);
        }
    }
}
