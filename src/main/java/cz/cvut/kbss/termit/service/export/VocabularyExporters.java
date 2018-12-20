package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.util.TypeAwareResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VocabularyExporters {

    private final List<VocabularyExporter> exporters;

    @Autowired
    public VocabularyExporters(List<VocabularyExporter> exporters) {
        this.exporters = exporters;
    }

    /**
     * Exports glossary of the specified vocabulary as the specified media type (if supported).
     * <p>
     * If the media type is not supported, an empty {@link Optional} is returned.
     *
     * @param vocabulary Vocabulary to export
     * @param mediaType  Target media type
     * @return Exported data wrapped in an {@code Optional}
     */
    public Optional<TypeAwareResource> exportVocabularyGlossary(Vocabulary vocabulary, String mediaType) {
        final Optional<VocabularyExporter> exporter = exporters.stream().filter(e -> e.supports(mediaType)).findFirst();
        return exporter.map(e -> e.exportVocabularyGlossary(vocabulary));
    }
}
