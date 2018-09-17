package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.persistence.dao.DocumentDao;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class DocumentRepositoryService extends BaseRepositoryService<Document> {

    private final DocumentDao dao;

    @Autowired
    public DocumentRepositoryService(DocumentDao dao, Validator validator) {
        super(validator);
        this.dao = dao;
    }

    @Override
    protected GenericDao<Document> getPrimaryDao() {
        return dao;
    }
}
