package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import org.springframework.core.io.Resource;

/**
 * Allows to export a vocabulary and assets related to it.
 */
@FunctionalInterface
public interface VocabularyExporter {

    /**
     * Gets a resource representation of the specified vocabulary's glossary.
     * <p>
     * The resource can be, for example, a CSV file.
     *
     * @param vocabulary Vocabulary whose glossary should be exported
     * @return IO resource representing the exported glossary
     */
    Resource exportVocabularyGlossary(Vocabulary vocabulary);
}
