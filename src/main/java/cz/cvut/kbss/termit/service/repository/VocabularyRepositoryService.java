package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Model;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.VocabularyService;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class VocabularyRepositoryService extends BaseAssetRepositoryService<Vocabulary> implements VocabularyService {

    private final IdentifierResolver idResolver;

    private final VocabularyDao vocabularyDao;

    @Autowired
    public VocabularyRepositoryService(VocabularyDao vocabularyDao, IdentifierResolver idResolver,
                                       Validator validator) {
        super(validator);
        this.vocabularyDao = vocabularyDao;
        this.idResolver = idResolver;
    }

    @Override
    protected AssetDao<Vocabulary> getPrimaryDao() {
        return vocabularyDao;
    }

    @Override
    protected void prePersist(Vocabulary instance) {
        super.prePersist(instance);
        if (instance.getUri() == null) {
            instance.setUri(idResolver.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, instance.getLabel()));
        }
        verifyIdentifierUnique(instance);
        if (instance.getGlossary() == null) {
            instance.setGlossary(new Glossary());
        }
        if (instance.getModel() == null) {
            instance.setModel(new Model());
        }
    }

    private void verifyIdentifierUnique(Vocabulary instance) {
        if (exists(instance.getUri())) {
            throw ResourceExistsException.create("Vocabulary", instance.getUri());
        }
    }
}
