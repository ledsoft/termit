package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Document;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentDao extends BaseDao<Document> {

    public DocumentDao(EntityManager em) {
        super(Document.class, em);
    }
}
