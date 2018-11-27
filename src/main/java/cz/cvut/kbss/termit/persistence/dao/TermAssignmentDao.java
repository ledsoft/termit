package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.util.Vocabulary;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository public class TermAssignmentDao extends BaseDao<TermAssignment> {

    @Autowired public TermAssignmentDao(EntityManager em) {
        super(TermAssignment.class, em);
    }

    /**
     * Finds all assignments of the specified terms.
     *
     * @param term Term whose assignments should be returned
     * @return List of matching assignments
     */
    public List<TermAssignment> findAll(Term term) {
        Objects.requireNonNull(term);
        return em.createNativeQuery("SELECT ?x WHERE {" + "?x a ?type ;" + "?hasTerm ?term . }",
            TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("term", term.getUri()).getResultList();
    }

    public List<TermAssignment> findByTarget(Target target) {
        Objects.requireNonNull(target);
        return em.createNativeQuery("SELECT ?x WHERE {" + "?x a ?type ;" + "?hasTarget ?target. }",
            TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("target", target.getUri()).getResultList();
    }
}
