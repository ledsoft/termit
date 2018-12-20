package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.util.TypeAwareResource;

/**
 * Allows to export a vocabulary and assets related to it.
 */
public interface VocabularyExporter {

    /**
     * Gets a resource representation of the specified vocabulary's glossary.
     * <p>
     * The resource can be, for example, a CSV file.
     *
     * @param vocabulary Vocabulary whose glossary should be exported
     * @return IO resource representing the exported glossary
     */
    TypeAwareResource exportVocabularyGlossary(Vocabulary vocabulary);

    /**
     * Checks whether this exporter supports the specified media type.
     *
     * @param mediaType Target media type for the export
     * @return Whether the media type is supported
     */
    boolean supports(String mediaType);
}
