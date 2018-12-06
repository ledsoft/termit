package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class ResourceDao extends BaseDao<Resource> {

    public ResourceDao(EntityManager em) {
        super(Resource.class, em);
    }

    /**
     * Gets terms the specified resource is annotated with.
     * <p>
     * The terms are ordered by their name (ascending).
     *
     * @param resource Annotated resource
     * @return List of terms annotating the specified resource
     */
    public List<Term> findTerms(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT ?x WHERE {" +
                "?x a ?term ;" +
                "?has-label ?label ." +
                "?assignment ?is-assignment-of ?x ;" +
                "?has-target/?has-source ?resource ." +
                "} ORDER BY ?label", Term.class)
                 .setParameter("term", URI.create(Vocabulary.s_c_term))
                 .setParameter("has-label", URI.create(RDFS.LABEL))
                 .setParameter("is-assignment-of", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("has-target", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("has-source", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }

    /**
     * Finds resources which are related to the specified one.
     * <p>
     * Two resources are related in this scenario if they have at least one common term assigned to them.
     * <p>
     * The returned resources are ordered by their name (ascending).
     *
     * @param resource Resource to filter by
     * @return List of resources related to the specified one
     */
    public List<Resource> findRelated(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?has-label ?label ." +
                "?assignment ?is-assignment-of ?term ;" +
                "?has-target/?has-source ?x ." +
                "?assignment2 ?is-assignment-of ?term ;" +
                "?has-target/?has-source ?resource ." +
                "FILTER (?x != ?resource)" +
                "} ORDER BY ?label", Resource.class)
                 .setParameter("type", typeUri)
                 .setParameter("has-label", URI.create(RDFS.LABEL))
                 .setParameter("is-assignment-of", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("has-target", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("has-source", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }
}
