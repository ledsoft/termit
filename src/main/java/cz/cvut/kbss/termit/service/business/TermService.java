package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.VocabularyExporters;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for term-related business logic.
 */
@Service
public class TermService {

    private final VocabularyExporters exporters;

    private final VocabularyService vocabularyService;

    private final TermRepositoryService repositoryService;

    @Autowired
    public TermService(VocabularyExporters exporters, VocabularyService vocabularyService,
                       TermRepositoryService repositoryService) {
        this.exporters = exporters;
        this.vocabularyService = vocabularyService;
        this.repositoryService = repositoryService;
    }

    /**
     * Attempts to export glossary terms from the specified vocabulary as the specified media type.
     * <p>
     * If export into the specified media type is not supported, an empty {@link Optional} is returned.
     *
     * @param vocabulary Vocabulary to export
     * @param mediaType  Expected media type of the export
     * @return Exported resource wrapped in an {@code Optional}
     */
    public Optional<TypeAwareResource> exportGlossary(Vocabulary vocabulary, String mediaType) {
        return exporters.exportVocabularyGlossary(vocabulary, mediaType);
    }

    /**
     * Retrieves all terms from the specified vocabulary.
     *
     * @param vocabulary Vocabulary whose terms will be returned
     * @return Matching terms
     */
    public List<Term> findAll(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return repositoryService.findAll(vocabulary);
    }

    /**
     * Retrieves root terms (terms without parent) from the specified vocabulary.
     * <p>
     * The page specification parameter allows configuration of the number of results and their offset.
     *
     * @param vocabulary Vocabulary whose terms will be returned
     * @param pageSpec   Paging specification
     * @return Matching terms
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return repositoryService.findAllRoots(vocabulary, pageSpec);
    }

    /**
     * Finds all root terms (terms without parent term) in the specified vocabulary or any of its imported
     * vocabularies.
     * <p>
     * Basically, this does a transitive closure over the vocabulary import relationship, starting at the specified
     * vocabulary, and returns all parent-less terms.
     *
     * @param vocabulary Base vocabulary for the vocabulary import closure
     * @param pageSpec   Page specifying result number and position
     * @return Matching root terms
     * @see #findAllRoots(Vocabulary, Pageable)
     */
    public List<Term> findAllRootsIncludingImports(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return repositoryService.findAllRootsIncludingImported(vocabulary, pageSpec);
    }

    /**
     * Retrieves root terms (terms without parent) in whose subterm tree (including themselves as the root) is a term
     * with label matching the specified search string.
     * <p>
     * Only terms from the specified vocabulary are matched.
     *
     * @param vocabulary   Vocabulary whose terms will be returned
     * @param searchString String by which to search in term labels
     * @return Matching terms
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, String searchString) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(searchString);
        return repositoryService.findAllRoots(vocabulary, searchString);
    }

    /**
     * Finds all root terms (terms without parent term) in the specified vocabulary or any vocabularies it imports
     * (transitively) whose sub-terms or themselves match the specified search string.
     *
     * @param vocabulary   Base vocabulary for the vocabulary import closure
     * @param searchString Search string
     * @return Match root terms
     * @see #findAllRoots(Vocabulary, String)
     */
    public List<Term> findAllRootsIncludingImports(Vocabulary vocabulary, String searchString) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(searchString);
        return repositoryService.findAllRootsIncludingImported(vocabulary, searchString);
    }

    /**
     * Gets vocabulary with the specified identifier.
     *
     * @param id Vocabulary identifier
     * @return Matching vocabulary
     * @throws NotFoundException When vocabulary with the specified identifier does not exist
     */
    public Vocabulary findVocabularyRequired(URI id) {
        Objects.requireNonNull(id);
        return vocabularyService.find(id)
                                .orElseThrow(() -> NotFoundException.create(Vocabulary.class.getSimpleName(), id));
    }

    /**
     * Gets a term with the specified identifier.
     *
     * @param id Term identifier
     * @return Matching term wrapped in an {@code Optional}
     */
    public Optional<Term> find(URI id) {
        return repositoryService.find(id);
    }

    /**
     * Gets a term with the specified identifier.
     *
     * @param id Term identifier
     * @return Matching term
     * @throws NotFoundException When no matching term is found
     */
    public Term findRequired(URI id) {
        return repositoryService.findRequired(id);
    }

    /**
     * Gets a reference to a Term with the specified identifier.
     *
     * @param id Term identifier
     * @return Matching Term reference wrapped in an {@code Optional}
     */
    public Optional<Term> getReference(URI id) {
        return repositoryService.getReference(id);
    }

    /**
     * Gets a reference to a Term with the specified identifier.
     *
     * @param id Term identifier
     * @return Matching term reference
     * @throws NotFoundException When no matching term is found
     */
    public Term getRequiredReference(URI id) {
        return repositoryService.getRequiredReference(id);
    }

    /**
     * Gets child terms of the specified parent term.
     *
     * @param parent Parent term whose children should be loaded
     * @return List of child terms
     */
    public List<Term> findSubTerms(Term parent) {
        Objects.requireNonNull(parent);
        return parent.getSubTerms() == null ? Collections.emptyList() :
               parent.getSubTerms().stream().map(u -> repositoryService.find(u.getUri()).orElseThrow(
                       () -> new NotFoundException(
                               "Child of term " + parent + " with id " + u.getUri() + " not found!")))
                     .collect(Collectors.toList());
    }

    /**
     * Gets aggregated info about assignments and occurrences of the specified Term.
     *
     * @param term Term whose assignments and occurrences to retrieve
     * @return List of term assignment describing instances
     */
    public List<TermAssignments> getAssignmentInfo(Term term) {
        return repositoryService.getAssignmentsInfo(term);
    }

    /**
     * Checks whether a term with the specified label already exists in the specified vocabulary.
     *
     * @param termLabel  Label to search for
     * @param vocabulary Vocabulary in which to search
     * @return Whether a matching label was found
     */
    public boolean existsInVocabulary(String termLabel, Vocabulary vocabulary) {
        Objects.requireNonNull(termLabel);
        Objects.requireNonNull(vocabulary);
        return repositoryService.existsInVocabulary(termLabel, vocabulary);
    }

    /**
     * Persists the specified term as a root term in the specified vocabulary's glossary.
     *
     * @param term  Term to persist
     * @param owner Vocabulary to add the term to
     */
    @Transactional
    public void persistRoot(Term term, Vocabulary owner) {
        Objects.requireNonNull(term);
        Objects.requireNonNull(owner);
        repositoryService.addRootTermToVocabulary(term, owner);
    }

    /**
     * Persists the specified term as a child of the specified parent term.
     *
     * @param child  The child to persist
     * @param parent Existing parent term
     */
    @Transactional
    public void persistChild(Term child, Term parent) {
        Objects.requireNonNull(child);
        Objects.requireNonNull(parent);
        repositoryService.addChildTerm(child, parent);
    }

    /**
     * Updates the specified term.
     *
     * @param term Term update data
     * @return The updated term
     */
    public Term update(Term term) {
        Objects.requireNonNull(term);
        return repositoryService.update(term);
    }

    /**
     * Removes the specified term.
     *
     * @param termUri Uri of the term
     */
    @Transactional
    public void remove(URI termUri) {
        repositoryService.remove(termUri);
    }

    /**
     * Generates identifier for a term with the specified label and in a vocabulary with the specified identifier.
     *
     * @param vocabularyUri Vocabulary identifier
     * @param termLabel     Term label
     * @return Generated term identifier
     */
    public URI generateIdentifier(URI vocabularyUri, String termLabel) {
        return repositoryService.generateIdentifier(vocabularyUri, termLabel);
    }
}
