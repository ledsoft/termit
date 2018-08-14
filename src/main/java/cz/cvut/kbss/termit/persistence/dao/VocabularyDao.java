package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VocabularyDao extends BaseDao<Vocabulary> {

    @Autowired
    public VocabularyDao(EntityManager em) {
        super(Vocabulary.class, em);
    }
}
