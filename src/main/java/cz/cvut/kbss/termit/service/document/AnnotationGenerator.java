package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.OccurrenceTarget;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.persistence.dao.TargetDao;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

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

    private final TargetDao targetDao;

    private final DocumentManager documentManager;

    private final TermOccurrenceResolvers resolvers;

    @Autowired
    public AnnotationGenerator(TermOccurrenceDao termOccurrenceDao,
                               TargetDao targetDao,
                               DocumentManager documentManager,
                               TermOccurrenceResolvers resolvers) {
        this.termOccurrenceDao = termOccurrenceDao;
        this.targetDao = targetDao;
        this.documentManager = documentManager;
        this.resolvers = resolvers;
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
        final TermOccurrenceResolver occurrenceResolver = findResolverFor(source);
        LOG.debug("Resolving annotations of file {}.", source);
        occurrenceResolver.parseContent(content, source);
        final List<TermOccurrence> occurrences = occurrenceResolver.findTermOccurrences();
        final List<TermOccurrence> existing = termOccurrenceDao.findAll(source);
        occurrences.stream().filter(o -> isNew(o, existing)).forEach(o -> {
            o.addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_navrzeny_vyskyt_termu);
            targetDao.persist(o.getTarget());
            termOccurrenceDao.persist(o);
        });
        saveAnnotatedContent(document, source, occurrenceResolver.getContent());
    }

    private TermOccurrenceResolver findResolverFor(File file) {
        // This will allow us to potentially support different types of files
        final TermOccurrenceResolver htmlResolver = resolvers.htmlTermOccurrenceResolver();
        if (htmlResolver.supports(file)) {
            return htmlResolver;
        } else {
            throw new AnnotationGenerationException("Unsupported type of file " + file);
        }
    }

    /**
     * Checks whether the specified term occurrence is new or if there already exists an equivalent one.
     * <p>
     * Two occurrences are considered equivalent iff they represent the same term, they have a target with the same
     * source file, and the target contains at least one equal selector.
     *
     * @param occurrence The supposedly new occurrence to check
     * @param existing   Existing occurrences relevant to the specified file
     * @return Whether the occurrence is truly new
     */
    private static boolean isNew(TermOccurrence occurrence, List<TermOccurrence> existing) {
        final OccurrenceTarget target = occurrence.getTarget();
        assert target != null;
        final Set<TermSelector> selectors = target.getSelectors();
        for (TermOccurrence to : existing) {
            if (!to.getTerm().equals(occurrence.getTerm())) {
                continue;
            }
            final OccurrenceTarget fileTarget = to.getTarget();
            assert fileTarget != null;
            assert fileTarget.getSource().equals(target.getSource());
            // Same term, contains at least one identical selector
            if (fileTarget.getSelectors().stream().anyMatch(selectors::contains)) {
                LOG.trace("Skipping occurrence {} because another one with matching term and selectors exists.",
                        occurrence);
                return false;
            }
        }
        return true;
    }

    private void saveAnnotatedContent(Document document, File file, InputStream input) {
        documentManager.saveFileContent(document, file, input);
    }
}
