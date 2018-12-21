package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.service.business.VocabularyService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Service
public class TermRepositoryService extends BaseRepositoryService<Term> {

    private final TermDao termDao;

    private final TermAssignmentDao termAssignmentDao;

    private final VocabularyService vocabularyService;

    public TermRepositoryService(Validator validator, TermDao termDao,
                                 TermAssignmentDao termAssignmentDao,
                                 VocabularyService vocabularyService) {
        super(validator);
        this.termDao = termDao;
        this.termAssignmentDao = termAssignmentDao;
        this.vocabularyService = vocabularyService;
    }

    @Override
    protected GenericDao<Term> getPrimaryDao() {
        return this.termDao;
    }

    @Transactional
    public void addTermToVocabulary(Term instance, Vocabulary vocabulary) {
        // TODO Fix update of glossary, it is not managed here
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);

        if (!vocabulary.getGlossary().addTerm(instance)) {
            throw ResourceExistsException.create("Term", instance.getUri());
        }
        termDao.persist(instance);
        // No need to explicitly merge glossary, it is managed during transaction and changes will be saved on commit
    }

    @Transactional
    public void addChildTerm(Term instance, Term parentTerm) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(parentTerm);

        if (!parentTerm.addSubTerm(instance.getUri())) {
            throw ResourceExistsException
                    .create("SubTerm " + instance.getUri() + "already exist in term " + parentTerm);
        }
        termDao.persist(instance);
        termDao.update(parentTerm);
    }

    /**
     * Gets all terms from a vocabulary, regardless of their position in the term hierarchy.
     * <p>
     * This returns all terms contained in a vocabulary's glossary.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @return List of terms ordered by label
     */
    public List<Term> findAll(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return termDao.findAll(vocabulary);
    }

    /**
     * Finds all root terms (terms without parent term) in the specified vocabulary.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @param pageSpec   Page specifying result number and position
     * @return Matching root terms
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return termDao.findAllRoots(vocabulary, pageSpec);
    }

    private Vocabulary getVocabulary(URI vocabularyUri) {
        return vocabularyService.find(vocabularyUri)
                                .orElseThrow(() -> NotFoundException
                                        .create(Vocabulary.class.getSimpleName(), vocabularyUri));
    }

    public List<Term> findAllRoots(String searchString, URI vocabularyUri) {
        // TODO remove
        Vocabulary vocabulary = getVocabulary(vocabularyUri);

        //TODO filter
        return termDao.findAllRoots(searchString, vocabulary);
    }

    public List<Term> findAllRoots(Vocabulary vocabulary, String searchString) {
        return termDao.findAllRoots(searchString, vocabulary);
    }

    /**
     * Checks whether a term with the specified label exists in a vocabulary with the specified URI.
     *
     * @param label      Label to check
     * @param vocabulary Vocabulary in which terms will be searched
     * @return Whether term with {@code label} already exists in vocabulary
     */
    public boolean existsInVocabulary(String label, Vocabulary vocabulary) {
        return termDao.existsInVocabulary(label, vocabulary);
    }

    /**
     * Retrieves all assignments of the specified term.
     *
     * @param instance Term whose assignments should be retrieved
     * @return List of term assignments (including term occurrences)
     */
    public List<TermAssignment> getAssignments(Term instance) {
        Objects.requireNonNull(instance);
        return termAssignmentDao.findAll(instance);
    }
}
