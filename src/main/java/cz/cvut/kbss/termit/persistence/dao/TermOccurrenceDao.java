package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermOccurrenceDao extends BaseDao<TermOccurrence> {

    @Autowired
    public TermOccurrenceDao(EntityManager em) {
        super(TermOccurrence.class, em);
    }

    /**
     * Finds all occurrences of the specified term.
     *
     * @param term Term whose occurrences should be returned
     * @return List of term occurrences
     */
    public List<TermOccurrence> findAll(Term term) {
        Objects.requireNonNull(term);
        return em.createNativeQuery("SELECT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTerm ?term . }", TermOccurrence.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("term", term.getUri()).getResultList();
    }

    /**
     * Finds all term occurrences whose target points to the specified resource.
     * <p>
     * I.e., these term occurrences appear in the specified resource (presumably file).
     *
     * @param resource Resource to filter by
     * @return List of matching term occurrences
     */
    public List<TermOccurrence> findAll(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTarget ?target ." +
                "?target ?hasSource ?resource . }", TermOccurrence.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }

    /**
     * Removes all suggested term occurrences whose target points to the specified resource.
     *
     * @param resource Resource for which suggested term occurrences will be removed
     */
    public void removeSuggested(Resource resource) {
        Objects.requireNonNull(resource);
        em.createNativeQuery("DELETE WHERE {" +
                "?x a ?suggestedOccurrence ;" +
                "a ?toType ;" +
                "?hasTerm ?term ;" +
                "?hasTarget ?target ." +
                "?target a ?occurrenceTarget ;" +
                "?hasSelector ?selector ;" +
                "?hasSource ?resource ." +
                "?selector ?sY ?sZ . }")
          .setParameter("suggestedOccurrence", URI.create(Vocabulary.s_c_navrzeny_vyskyt_termu))
          .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
          .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
          .setParameter("occurrenceTarget", URI.create(Vocabulary.s_c_cil_vyskytu))
          .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
          .setParameter("resource", resource.getUri())
          .setParameter("hasSelector", URI.create(Vocabulary.s_p_ma_selektor_termu)).executeUpdate();
    }
}
