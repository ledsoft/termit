package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
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

    private final TermRepositoryService termService;

    private final TermOccurrenceDao termOccurrenceDao;

    private final DocumentManager documentManager;

    private final TermOccurrenceResolver htmlOccurrenceResolver;

    @Autowired
    public AnnotationGenerator(TermRepositoryService termService,
                               TermOccurrenceDao termOccurrenceDao,
                               DocumentManager documentManager,
                               @Qualifier("html") TermOccurrenceResolver htmlOccurrenceResolver) {
        this.termService = termService;
        this.termOccurrenceDao = termOccurrenceDao;
        this.documentManager = documentManager;
        this.htmlOccurrenceResolver = htmlOccurrenceResolver;
    }

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     *
     * @param content  Content of file with identified term occurrences
     * @param source   Source file of the annotated document
     * @param document Logical document which contains content
     */
    @Transactional
    public void generateAnnotations(InputStream content, File source, Document document) {
        // This will allow us to potentially support different types of files
        if (htmlOccurrenceResolver.supports(source)) {
            LOG.debug("Resolving annotations of HTML file {}.", source);
            htmlOccurrenceResolver.parseContent(content, source);
            final List<Term> newTerms = htmlOccurrenceResolver.findNewTerms(document.getVocabulary());
            newTerms.forEach(t -> {
                t.addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_term);
                termService.addTermToVocabulary(t, document.getVocabulary().getUri());
            });
            final List<TermOccurrence> occurrences = htmlOccurrenceResolver.findTermOccurrences();
            occurrences.forEach(o -> {
                o.addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_vyskyt_termu);
                termOccurrenceDao.persist(o);
            });
            saveAnnotatedContent(document, source, htmlOccurrenceResolver.getContent());
        } else {
            throw new AnnotationGenerationException("Unsupported type of file " + source);
        }
    }

    private void saveAnnotatedContent(Document document, File file, InputStream input) {
        documentManager.saveFileContent(document, file, input);
    }
}
