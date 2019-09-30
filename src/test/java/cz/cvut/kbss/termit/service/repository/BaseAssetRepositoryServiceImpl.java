package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Validator;

public class BaseAssetRepositoryServiceImpl extends BaseAssetRepositoryService<Vocabulary> {

    private final VocabularyDao dao;

    @Autowired
    public BaseAssetRepositoryServiceImpl(VocabularyDao dao, Validator validator) {
        super(validator);
        this.dao = dao;
    }

    @Override
    protected AssetDao<Vocabulary> getPrimaryDao() {
        return dao;
    }
}
