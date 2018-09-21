package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;

@Repository
public class DocumentDao extends BaseDao<Document> {

    @Autowired
    public DocumentDao(EntityManager em) {
        super(Document.class, em);
    }

    @Override
    public List<Document> findAll() {
        try {
            return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?has-name ?name . } ORDER BY ?name", type)
                     .setParameter("type", typeUri)
                     .setParameter("has-name", URI.create(RDFS.LABEL))
                     .getResultList();
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
