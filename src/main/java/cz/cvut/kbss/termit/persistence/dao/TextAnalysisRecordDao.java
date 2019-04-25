package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.TextAnalysisRecord;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TextAnalysisRecordDao {

    private final EntityManager em;

    @Autowired
    public TextAnalysisRecordDao(EntityManager em) {
        this.em = em;
    }

    public void persist(TextAnalysisRecord record) {
        try {
            em.persist(record);
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Gets the latest {@link TextAnalysisRecord} for the specified Resource.
     *
     * @param resource Analyzed Resource
     * @return Latest analysis record, if it exists
     */
    public Optional<TextAnalysisRecord> findLatest(Resource resource) {
        Objects.requireNonNull(resource);
        try {
            return Optional.of(em.createNativeQuery("SELECT ?x WHERE { " +
                    "?x a ?type ;" +
                    "?hasResource ?resource ;" +
                    "?hasDateCreated ?dateCreated ." +
                    "} ORDER BY DESC(?dateCreated) LIMIT 1", TextAnalysisRecord.class)
                                 .setParameter("type", URI.create(
                                         "http://onto.fel.cvut.cz/ontologies/application/termit/pojem/záznam-o-textové-analýze"))
                                 .setParameter("hasResource", URI.create(
                                         "http://onto.fel.cvut.cz/ontologies/application/termit/pojem/má-analyzovaný-zdroj"))
                                 .setParameter("hasDateCreated", URI.create(Vocabulary.s_p_ma_datum_a_cas_vytvoreni))
                                 .setParameter("resource", resource.getUri()).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
