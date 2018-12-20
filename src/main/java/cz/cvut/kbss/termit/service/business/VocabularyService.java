package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Vocabulary;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Interface of business logic concerning vocabularies.
 */
public interface VocabularyService {

    /**
     * Gets all vocabularies in the system.
     *
     * @return List of vocabularies
     */
    List<Vocabulary> findAll();

    /**
     * Finds a vocabulary with the specified identifier.
     *
     * @param id Vocabulary identifier
     * @return Matching vocabulary wrapped in an {@code Optional}
     */
    Optional<Vocabulary> find(URI id);

    /**
     * Checks whether a vocabulary with the specified identifier exists.
     *
     * @param id Vocabulary identifier
     * @return Existence status
     */
    boolean exists(URI id);

    /**
     * Persists the specified vocabulary.
     *
     * @param vocabulary Vocabulary to save
     */
    void persist(Vocabulary vocabulary);

    /**
     * Updates the specified vocabulary.
     *
     * @param vocabulary Vocabulary with updated data
     * @return The updated vocabulary
     */
    Vocabulary update(Vocabulary vocabulary);
}
