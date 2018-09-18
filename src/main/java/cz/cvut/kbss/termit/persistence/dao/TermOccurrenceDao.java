package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
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
        try {
            return em.createNativeQuery("SELECT ?x WHERE {" +
                    "?x a ?type ;" +
                    "?hasTerm ?term . }", TermOccurrence.class)
                     .setParameter("type", typeUri)
                     .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_vyskytem_termu))
                     .setParameter("term", term.getUri()).getResultList();
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
