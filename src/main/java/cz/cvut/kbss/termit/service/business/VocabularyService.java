package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Vocabulary;

import java.net.URI;

/**
 * Interface of business logic concerning vocabularies.
 */
public interface VocabularyService extends CrudService<Vocabulary> {

    /**
     * Generates a vocabulary identifier based on the specified label.
     *
     * @param label Vocabulary label
     * @return Vocabulary identifier
     */
    URI generateIdentifier(String label);
}
