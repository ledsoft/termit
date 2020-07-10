/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Service
public class TermRepositoryService extends BaseAssetRepositoryService<Term> {

    private final IdentifierResolver idResolver;

    private final Configuration config;

    private final TermDao termDao;

    private final TermAssignmentDao termAssignmentDao;

    private final VocabularyRepositoryService vocabularyService;

    public TermRepositoryService(Validator validator, IdentifierResolver idResolver,
                                 Configuration config, TermDao termDao, TermAssignmentDao termAssignmentDao,
                                 VocabularyRepositoryService vocabularyService) {
        super(validator);
        this.idResolver = idResolver;
        this.config = config;
        this.termDao = termDao;
        this.termAssignmentDao = termAssignmentDao;
        this.vocabularyService = vocabularyService;
    }

    @Override
    protected AssetDao<Term> getPrimaryDao() {
        return this.termDao;
    }

    @Override
    public void persist(Term instance) {
        throw new UnsupportedOperationException(
                "Persisting term by itself is not supported. It has to be connected to a vocabulary or a parent term.");
    }

    @Override
    protected void postUpdate(Term instance) {
        final Vocabulary vocabulary = vocabularyService.getRequiredReference(instance.getVocabulary());
        if (instance.hasParentInSameVocabulary()) {
            vocabulary.getGlossary().removeRootTerm(instance);
        } else {
            vocabulary.getGlossary().addRootTerm(instance);
        }
    }

    @Transactional
    public void addRootTermToVocabulary(Term instance, Vocabulary vocabulary) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);

        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(vocabulary.getUri(), instance.getLabel()));
        }
        verifyIdentifierUnique(instance);
        instance.setGlossary(vocabulary.getGlossary().getUri());
        addTermAsRootToGlossary(instance, vocabulary.getUri());
        termDao.persist(instance, vocabulary);
    }

    /**
     * Generates term identifier based on the specified parent vocabulary identifier and a term label.
     *
     * @param vocabularyUri Vocabulary identifier
     * @param termLabel     Term label
     * @return Generated term identifier
     */
    public URI generateIdentifier(URI vocabularyUri, String termLabel) {
        Objects.requireNonNull(vocabularyUri);
        Objects.requireNonNull(termLabel);
        return idResolver.generateIdentifier(
                idResolver.buildNamespace(vocabularyUri.toString(), config.get(ConfigParam.TERM_NAMESPACE_SEPARATOR)),
                termLabel);
    }

    private void addTermAsRootToGlossary(Term instance, URI vocabularyIri) {
        // Load vocabulary so that it is managed and changes to it (resp. the glossary) are persisted on commit
        final Vocabulary toUpdate = vocabularyService.getRequiredReference(vocabularyIri);
        instance.setGlossary(toUpdate.getGlossary().getUri());
        toUpdate.getGlossary().addRootTerm(instance);
    }

    @Transactional
    public void addChildTerm(Term instance, Term parentTerm) {
        validate(instance);
        Objects.requireNonNull(instance);
        Objects.requireNonNull(parentTerm);
        final URI vocabularyIri =
                instance.getVocabulary() != null ? instance.getVocabulary() : parentTerm.getVocabulary();
        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(vocabularyIri, instance.getLabel()));
        }
        verifyIdentifierUnique(instance);

        instance.addParentTerm(parentTerm);
        if (!instance.hasParentInSameVocabulary()) {
            addTermAsRootToGlossary(instance, vocabularyIri);
        }

        termDao.persist(instance, vocabularyService.getRequiredReference(vocabularyIri));
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
     * @see #findAllRootsIncludingImported(Vocabulary, Pageable)
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        return termDao.findAllRoots(vocabulary, pageSpec);
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
    public List<Term> findAllRootsIncludingImported(Vocabulary vocabulary, Pageable pageSpec) {
        return termDao.findAllRootsIncludingImports(vocabulary, pageSpec);
    }

    /**
     * Finds all terms which match the specified search string in the specified vocabulary.
     *
     * @param searchString Search string
     * @param vocabulary   Vocabulary whose terms should be returned
     * @return Matching terms
     */
    public List<Term> findAll(String searchString, Vocabulary vocabulary) {
        return termDao.findAll(searchString, vocabulary);
    }

    /**
     * Finds all terms which match the specified search string in the specified vocabulary and any vocabularies it
     * (transitively) imports.
     *
     * @param searchString Search string
     * @param vocabulary   Vocabulary whose terms should be returned
     * @return Matching terms
     */
    public List<Term> findAllIncludingImported(String searchString, Vocabulary vocabulary) {
        return termDao.findAllIncludingImported(searchString, vocabulary);
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
     * Retrieves aggregated information about the specified Term's assignments to and occurrences in {@link
     * cz.cvut.kbss.termit.model.resource.Resource}s.
     *
     * @param instance Term whose assignment/occurrence data should be retrieved
     * @return Aggregated Term assignment/occurrence data
     */
    public List<TermAssignments> getAssignmentsInfo(Term instance) {
        return termAssignmentDao.getAssignmentInfo(instance);
    }
}
