package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermAssignments;
import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermAssignmentDao extends BaseDao<TermAssignment> {

    private static final String ASSIGNMENTS_CONDITION = "{" +
            "  ?x a ?suggestedAssignment ." +
            "  BIND (true as ?suggested)" +
            "  } UNION {" +
            "  ?x a ?assignment ." +
            "  FILTER NOT EXISTS {" +
            "    ?x a ?type ." +
            "    ?type rdfs:subClassOf+ ?assignment ." +
            "    FILTER (?type != ?assignment) " +
            "  }" +
            "  BIND (false as ?suggested)" +
            "  }" +
            "  ?x ?hasTerm ?term ;" +
            "    ?hasTarget/?hasSource ?resource.";

    private static final String OCCURRENCES_CONDITION = "{" +
            "  ?x a ?suggestedOccurrence ." +
            "  BIND (true as ?suggested)" +
            "} UNION {" +
            "  ?x a ?occurrence ." +
            "  FILTER NOT EXISTS {" +
            "    ?x a ?suggestedOccurrence ." +
            "  }" +
            "  BIND (false as ?suggested)" +
            "} " +
            "  ?x ?hasTerm ?term ;" +
            "    ?hasTarget/?hasSource ?resource.";

    @Autowired
    public TermAssignmentDao(EntityManager em) {
        super(TermAssignment.class, em);
    }

    /**
     * Gets information about assignments and occurrences of the specified {@link Term}.
     *
     * @param term Term whose assignments and occurrences to retrieve
     * @return List of {@code TermAssignments} and {@code TermOccurrences}
     */
    public List<TermAssignments> getAssignmentInfo(Term term) {
        Objects.requireNonNull(term);
        final List<TermAssignments> lst = getAssignments(term);
        lst.addAll(getOccurrences(term));
        return lst;
    }

    private List<TermAssignments> getAssignments(Term term) {
        return em.createNativeQuery("SELECT DISTINCT ?term ?resource ?label ?suggested WHERE {" +
                "BIND (?t AS ?term)" +
                ASSIGNMENTS_CONDITION +
                "  ?resource rdfs:label ?label ." +
                "} ORDER BY ?label", "TermAssignments")
                 .setParameter("suggestedAssignment", URI.create(Vocabulary.s_c_navrzene_prirazeni_termu))
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("assignment", URI.create(Vocabulary.s_c_prirazeni_termu))
                 .setParameter("t", term.getUri()).getResultList();
    }

    private List<TermAssignments> getOccurrences(Term term) {
        return em.createNativeQuery("SELECT ?term ?resource ?label (count(?x) as ?cnt) ?suggested WHERE {" +
                        "BIND (?t AS ?term)" +
                        OCCURRENCES_CONDITION +
                        "  ?resource rdfs:label ?label ." +
                        "} GROUP BY ?resource ?term ?label ?suggested HAVING (?cnt > 0) ORDER BY ?label",
                "TermOccurrences")
                 .setParameter("suggestedOccurrence", URI.create(Vocabulary.s_c_navrzeny_vyskyt_termu))
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("occurrence", URI.create(Vocabulary.s_c_vyskyt_termu))
                 .setParameter("t", term.getUri()).getResultList();
    }

    public List<TermAssignment> findByTarget(Target target) {
        Objects.requireNonNull(target);
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasTarget ?target. }",
                TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("target", target.getUri()).getResultList();
    }

    /**
     * Finds all assignments whose target represents this resource.
     * <p>
     * This includes both term assignments and term occurrences.
     *
     * @param resource Target resource to filter by
     * @return List of matching assignments
     */
    public List<TermAssignment> findAll(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasTarget/?hasSource ?resource. }",
                TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }

    /**
     * Gets information about term occurrences and assignments for the specified resource.
     *
     * @param resource Resource for which Term occurrences and assignments info should be retrieved
     * @return List of {@code ResourceTermAssignments} and {@code ResourceTermOccurrences}
     */
    public List<ResourceTermAssignments> getAssignmentInfo(Resource resource) {
        final List<ResourceTermAssignments> assignments = getAssignments(resource);
        final List<ResourceTermAssignments> occurrences = getOccurrences(resource);
        assignments.addAll(occurrences);
        return assignments;
    }

    private List<ResourceTermAssignments> getAssignments(Resource resource) {
        return em.createNativeQuery("SELECT DISTINCT ?term ?label ?vocabulary ?res ?suggested WHERE {" +
                ASSIGNMENTS_CONDITION +
                "  ?term rdfs:label ?label ;" +
                "  ?inVocabulary ?vocabulary ." +
                "BIND (?resource AS ?res)" +
                "} ORDER BY ?label", "ResourceTermAssignments")
                 .setParameter("suggestedAssignment", URI.create(Vocabulary.s_c_navrzene_prirazeni_termu))
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("inVocabulary", URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .setParameter("assignment", URI.create(Vocabulary.s_c_prirazeni_termu))
                 .setParameter("resource", resource.getUri()).getResultList();
    }

    private List<ResourceTermAssignments> getOccurrences(Resource resource) {
        return em.createNativeQuery("SELECT ?term ?label ?vocabulary (count(?x) as ?cnt) ?res ?suggested WHERE {" +
                        OCCURRENCES_CONDITION +
                        "  ?term rdfs:label ?label ;" +
                        "    ?inVocabulary ?vocabulary ." +
                        "BIND (?resource AS ?res)" +
                        "} GROUP BY ?term ?label ?vocabulary ?res ?suggested HAVING (?cnt > 0) ORDER BY ?label",
                "ResourceTermOccurrences")
                 .setParameter("suggestedOccurrence", URI.create(Vocabulary.s_c_navrzeny_vyskyt_termu))
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("inVocabulary", URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .setParameter("occurrence", URI.create(Vocabulary.s_c_vyskyt_termu))
                 .setParameter("resource", resource.getUri()).getResultList();
    }
}
