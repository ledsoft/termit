package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Term;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;

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

}