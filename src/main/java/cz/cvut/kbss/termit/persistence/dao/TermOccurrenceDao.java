package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.resource.File;
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
        return em.createNativeQuery("SELECT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTerm ?term . }", TermOccurrence.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_vyskytem_termu))
                 .setParameter("term", term.getUri()).getResultList();
    }

    /**
     * Finds all term occurrences which have at least one target pointing to the specified file.
     * <p>
     * I.e., these term occurrences appear in the specified file.
     *
     * @param file File to filter by
     * @return List of matching term occurrences
     */
    public List<TermOccurrence> findAllInFile(File file) {
        Objects.requireNonNull(file);
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTarget ?target ." +
                "?target ?hasSource ?file . }", TermOccurrence.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdrojovy_dokument))
                 .setParameter("file", file.getUri()).getResultList();
    }
}
