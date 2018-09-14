package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;

@Repository
public class TermDao extends BaseDao<Term> {

    @Autowired
    public TermDao(EntityManager em) {
        super(Term.class, em);
    }

    /**
     * Loads a page of terms contained in the specified vocabulary.
     *
     * @param pageSpec   Page specification
     * @param vocabulary Vocabulary whose terms should be returned
     * @return Matching terms, ordered by their label
     */
    public List<Term> findAll(Pageable pageSpec, Vocabulary vocabulary) {
        return em.createNativeQuery("SELECT ?term WHERE {" +
                "?term a ?type ;" +
                "rdfs:label ?label ." +
                "?vocabulary ?hasGlossary/?hasTerm ?term ." +
                "} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasGlossary", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar))
                 .setParameter("hasTerm", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_obsahuje_pojem))
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setUntypedParameter("offset", pageSpec.getOffset())
                 .setUntypedParameter("limit", pageSpec.getPageSize())
                 .getResultList();
    }

    public List<Term> find(String termLabel, URI vocabularyUri, URI parentTermUri, Integer offset, Integer limit) {
        //TODO query
        // https://blog.novatec-gmbh.de/art-pagination-offset-vs-value-based-paging/
        throw new NotImplementedException();
    }

}