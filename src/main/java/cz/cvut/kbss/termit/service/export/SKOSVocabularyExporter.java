package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.skos.SKOSExporter;
import cz.cvut.kbss.termit.service.export.util.TypeAwareByteArrayResource;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static cz.cvut.kbss.termit.util.Constants.Turtle.FILE_EXTENSION;
import static cz.cvut.kbss.termit.util.Constants.Turtle.MEDIA_TYPE;

/**
 * Exports vocabulary glossary in a SKOS-compatible format.
 */
@Service("skos")
public class SKOSVocabularyExporter implements VocabularyExporter {

    private final ApplicationContext context;

    @Autowired
    public SKOSVocabularyExporter(ApplicationContext context) {
        this.context = context;
    }

    /**
     * This method ensures new instances of the prototype-scoped bean are returned on every call.
     */
    private SKOSExporter getSKOSExporter() {
        return context.getBean(SKOSExporter.class);
    }

    @Override
    @Transactional
    public TypeAwareResource exportVocabularyGlossary(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final SKOSExporter skosExporter = getSKOSExporter();
        skosExporter.exportGlossaryInstance(vocabulary);
        return new TypeAwareByteArrayResource(skosExporter.exportAsTtl(), MEDIA_TYPE, FILE_EXTENSION);
    }

    @Override
    public boolean supports(String mediaType) {
        return Objects.equals(MEDIA_TYPE, mediaType);
    }
}
