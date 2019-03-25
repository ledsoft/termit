package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.List;

/**
 * Base DAO implementation for assets managed by the application.
 *
 * @param <T> Type of the asset
 */
public abstract class AssetDao<T extends Asset> extends BaseDao<T> {

    AssetDao(Class<T> type, EntityManager em) {
        super(type, em);
    }

    /**
     * Finds the specified number of most recently added/edited assets.
     *
     * @param limit Number of assets to load
     * @return List of recently added/edited assets
     */
    public List<T> findLastEdited(int limit) {
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?dateCreated ?created ." +
                "OPTIONAL { ?x ?lastModified ?modified . }" +
                "BIND (IF (BOUND(?modified), ?modified, ?created) AS ?lastEdited)" +
                "} ORDER BY DESC(?lastEdited) LIMIT ?limit", type)
                 .setParameter("type", typeUri)
                 .setParameter("dateCreated", URI.create(Vocabulary.s_p_ma_datum_a_cas_vytvoreni))
                 .setParameter("lastModified", URI.create(Vocabulary.s_p_ma_datum_a_cas_posledni_modifikace))
                 .setUntypedParameter("limit", limit).getResultList();
    }
}
