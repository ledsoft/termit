package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    @Transactional
    public void addTermToVocabulary(Term instance, URI vocabularyUri) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabularyUri);
        final Vocabulary vocabulary = getVocabulary(vocabularyUri);

        if (!vocabulary.getGlossary().addTerm(instance)) {
            throw ResourceExistsException.create("Term", instance.getUri());
        }
        vocabularyService.update(vocabulary);
    }

    @Transactional
    public void addTermToVocabulary(Term instance, URI vocabularyUri, URI parentTermUri) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabularyUri);
        Objects.requireNonNull(parentTermUri);
        final Vocabulary vocabulary = getVocabulary(vocabularyUri);

        if (!vocabulary.getGlossary().addTerm(instance)) {
            throw ResourceExistsException.create("Term", instance.getUri());
        }

        Term parenTerm = find(parentTermUri).orElseThrow(() -> NotFoundException.create("Term", parentTermUri));

        Set<URI> newTerms = parenTerm.getSubTerms();
        if (newTerms == null) {
            newTerms = new HashSet<>();
        }
        if (!newTerms.add(instance.getUri())) {
            throw ResourceExistsException
                    .create("SubTerm " + instance.getUri() + "already exist in term " + parentTermUri);
        }
        parenTerm.setSubTerms(newTerms);

        vocabularyService.update(vocabulary);
    }

    public List<Term> findAll(URI vocabularyUri, int limit, int offset) {
        Vocabulary vocabulary = getVocabulary(vocabularyUri);

        return termDao.findAll(limit, offset, vocabulary);
    }

    private Vocabulary getVocabulary(URI vocabularyUri) {
        return vocabularyService.find(vocabularyUri)
                                .orElseThrow(() -> NotFoundException
                                        .create(Vocabulary.class.getSimpleName(), vocabularyUri));
    }

    public List<Term> findAll(String searchString, URI vocabularyUri) {
        Vocabulary vocabulary = getVocabulary(vocabularyUri);

        //TODO filter
        return termDao.findAll(searchString, vocabulary);
    }

    /**
     * Checks whether a term with the specified label exists in a vocabulary with the specified URI.
     *
     * @param label         Label to check
     * @param vocabularyUri Vocabulary in which terms will be searched
     * @return Whether term with {@code label} already exists in vocabulary
     */
    public boolean existsInVocabulary(String label, URI vocabularyUri) {
        return termDao.existsInVocabulary(label, vocabularyUri);
    }
}
