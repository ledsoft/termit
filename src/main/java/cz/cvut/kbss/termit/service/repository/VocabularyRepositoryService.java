package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.IdentifierUtils;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.Date;

@Service
public class VocabularyRepositoryService extends BaseRepositoryService<Vocabulary> {

    private final SecurityUtils securityUtils;

    private final Configuration config;

    private final VocabularyDao vocabularyDao;

    @Autowired
    public VocabularyRepositoryService(VocabularyDao vocabularyDao, SecurityUtils securityUtils, Configuration config,
                                       Validator validator) {
        super(validator);
        this.vocabularyDao = vocabularyDao;
        this.securityUtils = securityUtils;
        this.config = config;
    }

    @Override
    protected GenericDao<Vocabulary> getPrimaryDao() {
        return vocabularyDao;
    }

    @Override
    protected void prePersist(Vocabulary instance) {
        validate(instance);
        instance.setDateCreated(new Date());
        instance.setAuthor(securityUtils.getCurrentUser());
        if (instance.getUri() == null) {
            instance.setUri(IdentifierUtils
                    .generateIdentifier(config.get(ConfigParam.VOCABULARY_BASE_IRI), instance.getName()));
        }
    }

    @Override
    protected Vocabulary postLoad(Vocabulary instance) {
        instance.getAuthor().erasePassword();
        return instance;
    }
}
