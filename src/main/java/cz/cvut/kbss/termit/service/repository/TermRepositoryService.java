package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Set;

@Service
public class TermRepositoryService extends BaseRepositoryService<Term> {

    private final TermDao termDao;

    private final VocabularyRepositoryService vocabularyService;

    public TermRepositoryService(Validator validator, TermDao termDao, VocabularyRepositoryService vocabularyService) {
        super(validator);
        this.termDao = termDao;
        this.vocabularyService = vocabularyService;
    }

    @Override
    protected GenericDao<Term> getPrimaryDao() {
        return this.termDao;
    }

    public void addTermToVocabulary(Term instance, URI vocabularyUri, URI parentTermUri) {
        validate(instance);
        Vocabulary vocabulary = vocabularyService.find(vocabularyUri)
                                                 .orElseThrow(() -> NotFoundException
                                                         .create(Vocabulary.class.getSimpleName(), vocabularyUri));
        //TODO custom comparator for the Set<Term> -> compare only uri not the objects
        if (!vocabulary.getGlossary().getTerms().add(instance)) {
            throw ResourceExistsException.create("Term", instance.getUri());
        }

        if (parentTermUri != null) {
            Term parenTerm = find(parentTermUri).orElseThrow(() -> NotFoundException.create("Term", parentTermUri));

            Set<Term> newTerms = parenTerm.getSubTerms();
            newTerms.add(instance);
            parenTerm.setSubTerms(newTerms);
        }

        vocabularyService.update(vocabulary);
    }

    public List<Term> findAll(URI vocabularyUri, int limit, int offset) {
        Vocabulary vocabulary = vocabularyService.find(vocabularyUri)
                                                 .orElseThrow(() -> NotFoundException
                                                         .create(Vocabulary.class.getSimpleName(), vocabularyUri));
        Pageable pageable = PageRequest.of(offset, limit);

        return termDao.findAll(pageable, vocabulary);
    }

    public List<Term> findAll(String searchString, URI vocabularyUri) {
        Vocabulary vocabulary = vocabularyService.find(vocabularyUri)
                                                 .orElseThrow(() -> NotFoundException
                                                         .create(Vocabulary.class.getSimpleName(), vocabularyUri));

        List<Term> rootTerms = termDao.findAll(searchString, vocabulary);
        // List<Term> allTerms = new ArrayList<>(rootTerms.size()*4);
        // TODO if term.subTerms will be FetchType.LAZY then fetch all children, filter them and return the result
        return rootTerms;
    }
}
