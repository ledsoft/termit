package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Term;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermDao extends BaseDao<Term> {

    @Autowired
    public TermDao(EntityManager em) {
        super(Term.class, em);
    }


    public List<Term> find(String termLabel, URI vocabularyUri, URI parentTermUri, Integer offset, Integer limit) {
        //TODO query
        // https://blog.novatec-gmbh.de/art-pagination-offset-vs-value-based-paging/
        throw new NotImplementedException();
    }

    /**
     * Checks whether a term with the specified label exists in specific vocabulary.
     *
     * @param label Term label to check
     * @return {@code true} if a user with the specified username exists
     */
    public boolean exists(String label, URI vocabularyUri) {
        Objects.requireNonNull(label);
        //TODO query
        throw new NotImplementedException();
    }
}