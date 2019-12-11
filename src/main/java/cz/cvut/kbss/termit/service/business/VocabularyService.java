package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.asset.provenance.SupportsLastModification;
import cz.cvut.kbss.termit.model.Vocabulary;

import java.net.URI;
import java.util.Collection;

/**
 * Interface of business logic concerning vocabularies.
 */
public interface VocabularyService extends CrudService<Vocabulary>, SupportsLastModification {

    /**
     * Generates a vocabulary identifier based on the specified label.
     *
     * @param label Vocabulary label
     * @return Vocabulary identifier
     */
    URI generateIdentifier(String label);

    /**
     * Gets identifiers of all vocabularies imported by the specified vocabulary, including transitively imported ones.
     *
     * @param entity Base vocabulary, whose imports should be retrieved
     * @return Collection of (transitively) imported vocabularies
     */
    Collection<URI> getTransitivelyImportedVocabularies(Vocabulary entity);
}
