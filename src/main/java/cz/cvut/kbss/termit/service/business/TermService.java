package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Service for term-related business logic.
 */
public class TermService {

    /**
     * Attempts to export glossary terms from the specified vocabulary as the specified media type.
     * <p>
     * If export into the specified media type is not supported, an empty {@link Optional} is returned.
     *
     * @param vocabulary Vocabulary to export
     * @param mediaType  Expected media type of the export
     * @return Exported resource wrapped in an {@code Optional}
     */
    public Optional<Resource> exportGlossary(Vocabulary vocabulary, String mediaType) {
        return null;
    }

    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        return null;
    }

    public List<Term> findAllRoots(Vocabulary vocabulary, String searchString) {
        return null;
    }

    public Optional<Vocabulary> findVocabulary(URI id) {
        return null;
    }

    public Optional<Term> find(URI id) {
        return null;
    }

    public List<Term> findSubTerms(Term parent) {
        return null;
    }

    public List<Term> findSubTerms(Vocabulary vocabulary, String searchString) {
        return null;
    }

    public List<TermAssignment> getAssignments(Term term) {
        return null;
    }

    public boolean existsInVocabulary(String termLabel) {
        return false;
    }

    public void persist(Vocabulary owner, Term term) {

    }

    public void persist(Term parent, Term term) {

    }

    public Term update(Term term) {
        return term;
    }
}
