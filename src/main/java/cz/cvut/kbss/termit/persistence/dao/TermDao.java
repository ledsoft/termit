/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.dto.TermInfo;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.DescriptorFactory;
import cz.cvut.kbss.termit.persistence.PersistenceUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class TermDao extends AssetDao<Term> {

    private static final URI LABEL_PROP = URI.create(SKOS.PREF_LABEL);

    private final PersistenceUtils persistenceUtils;

    @Autowired
    public TermDao(EntityManager em, Configuration config, DescriptorFactory descriptorFactory,
                   PersistenceUtils persistenceUtils) {
        super(Term.class, em, config, descriptorFactory);
        this.persistenceUtils = persistenceUtils;
    }

    @Override
    protected URI labelProperty() {
        return LABEL_PROP;
    }

    @Override
    public Optional<Term> find(URI id) {
        final Optional<Term> result = super.find(id);
        result.ifPresent(this::loadSubTerms);
        return result;
    }

    @Override
    public void persist(Term entity) {
        Objects.requireNonNull(entity);
        assert entity.getVocabulary() != null;

        try {
            em.persist(entity, descriptorFactory.termDescriptor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Term update(Term entity) {
        Objects.requireNonNull(entity);
        assert entity.getVocabulary() != null;

        try {
            // Evict possibly cached instance loaded from default context
            em.getEntityManagerFactory().getCache().evict(Term.class, entity.getUri(), null);
            return em.merge(entity, descriptorFactory.termDescriptor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Gets all terms on the specified vocabulary.
     * <p>
     * No differences are made between root terms and terms with parents.
     *
     * @param vocabulary Vocabulary whose terms should be returned
     * @return Matching terms, ordered by label
     */
    public List<Term> findAll(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        try {
            return executeQueryAndLoadSubTerms(em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                    "GRAPH ?g { " +
                    "?term a ?type ;" +
                    "?hasLabel ?label ;" +
                    "?inVocabulary ?vocabulary ." +
                    "FILTER (lang(?label) = ?labelLang) ." +
                    "} } ORDER BY ?label", Term.class)
                                                 .setParameter("type", typeUri)
                                                 .setParameter("vocabulary", vocabulary.getUri())
                                                 .setParameter("g",
                                                         persistenceUtils.resolveVocabularyContext(vocabulary.getUri()))
                                                 .setParameter("hasLabel", LABEL_PROP)
                                                 .setParameter("inVocabulary",
                                                         URI.create(
                                                                 cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                                                 .setParameter("labelLang", config.get(ConfigParam.LANGUAGE)));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    private List<Term> executeQueryAndLoadSubTerms(TypedQuery<Term> query) {
        final List<Term> terms = query.getResultList();
        terms.forEach(this::loadSubTerms);
        return terms;
    }

    /**
     * Loads sub-term info for the specified parent term.
     * <p>
     * The sub-terms are set directly on the specified parent.
     *
     * @param parent Parent term
     */
    private void loadSubTerms(Term parent) {
        final Stream<TermInfo> subTermsStream = em.createNativeQuery("SELECT ?entity ?label ?vocabulary WHERE {" +
                "?parent ?narrower ?entity ." +
                "?entity a ?type ;" +
                "?hasLabel ?label ;" +
                "?inVocabulary ?vocabulary ." +
                "FILTER (lang(?label) = ?labelLang) . }", "TermInfo")
                                                  .setParameter("type", typeUri)
                                                  .setParameter("narrower", URI.create(SKOS.NARROWER))
                                                  .setParameter("parent", parent.getUri())
                                                  .setParameter("hasLabel", LABEL_PROP)
                                                  .setParameter("inVocabulary",
                                                          URI.create(
                                                                  cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                                                  .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                                                  .getResultStream();
        parent.setSubTerms(subTermsStream.collect(Collectors.toSet()));
    }

    /**
     * Loads a page of root terms (terms without a parent) contained in the specified vocabulary.
     *
     * @param vocabulary Vocabulary whose root terms should be returned
     * @param pageSpec   Page specification
     * @return Matching terms, ordered by their label
     * @see #findAllRootsIncludingImports(Vocabulary, Pageable)
     */
    public List<Term> findAllRoots(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "GRAPH ?g { " +
                "?term a ?type ;" +
                "?hasLabel ?label ." +
                "?vocabulary ?hasGlossary/?hasTerm ?term ." +
                "FILTER (lang(?label) = ?labelLang) ." +
                "}} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class);
        query = setCommonFindAllRootsQueryParams(query, false);
        try {
            return executeQueryAndLoadSubTerms(query.setParameter("vocabulary", vocabulary.getUri())
                                                    .setParameter("g",
                                                            persistenceUtils.resolveVocabularyContext(vocabulary.getUri()))
                                                    .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                                                    .setUntypedParameter("offset", pageSpec.getOffset())
                                                    .setUntypedParameter("limit", pageSpec.getPageSize()));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    private <T> TypedQuery<T> setCommonFindAllRootsQueryParams(TypedQuery<T> query, boolean includeImports) {
        final TypedQuery<T> tq = query.setParameter("type", typeUri)
                                      .setParameter("hasLabel", LABEL_PROP)
                                      .setParameter("hasGlossary",
                                              URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar))
                                      .setParameter("hasTerm", URI.create(SKOS.HAS_TOP_CONCEPT));
        if (includeImports) {
            tq.setParameter("imports", URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik));
        }
        return tq;
    }

    /**
     * Loads a page of root terms contained in the specified vocabulary or any of its imports (transitively).
     * <p>
     * This method basically does a transitive closure of the vocabulary import relationship and retrieves a page of
     * root terms from this closure.
     *
     * @param vocabulary The last vocabulary in the vocabulary import chain
     * @param pageSpec   Page specification
     * @return Matching terms, ordered by their label
     * @see #findAllRoots(Vocabulary, Pageable)
     */
    public List<Term> findAllRootsIncludingImports(Vocabulary vocabulary, Pageable pageSpec) {
        Objects.requireNonNull(vocabulary);
        Objects.requireNonNull(pageSpec);
        TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "?term a ?type ;" +
                "?hasLabel ?label ." +
                "?vocabulary ?imports* ?parent ." +
                "?parent ?hasGlossary/?hasTerm ?term ." +
                "FILTER (lang(?label) = ?labelLang) ." +
                "} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class);
        query = setCommonFindAllRootsQueryParams(query, true);
        try {
            return executeQueryAndLoadSubTerms(query.setParameter("vocabulary", vocabulary.getUri())
                                                    .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                                                    .setUntypedParameter("offset", pageSpec.getOffset())
                                                    .setUntypedParameter("limit", pageSpec.getPageSize()));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Finds terms whose label contains the specified search string.
     * <p>
     * This method searches in the specified vocabulary only.
     *
     * @param searchString String the search term labels by
     * @param vocabulary   Vocabulary whose terms should be searched
     * @return List of matching terms
     */
    public List<Term> findAll(String searchString, Vocabulary vocabulary) {
        Objects.requireNonNull(searchString);
        Objects.requireNonNull(vocabulary);
        final TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "GRAPH ?g { " +
                "?term a ?type ; " +
                "      ?hasLabel ?label ; " +
                "      ?inVocabulary ?vocabulary ." +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) ." +
                "}} ORDER BY ?label", Term.class)
                                         .setParameter("type", typeUri)
                                         .setParameter("hasLabel", LABEL_PROP)
                                         .setParameter("inVocabulary", URI.create(
                                                 cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                                         .setParameter("vocabulary", vocabulary.getUri())
                                         .setParameter("g",
                                                 persistenceUtils.resolveVocabularyContext(vocabulary.getUri()))
                                         .setParameter("searchString", searchString, config.get(ConfigParam.LANGUAGE));
        try {
            final List<Term> terms = executeQueryAndLoadSubTerms(query);
            terms.forEach(this::loadParentSubTerms);
            return terms;
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    private void loadParentSubTerms(Term parent) {
        loadSubTerms(parent);
        if (parent.getParentTerms() != null) {
            parent.getParentTerms().forEach(this::loadParentSubTerms);
        }
    }

    /**
     * Finds terms whose label contains the specified search string.
     * <p>
     * This method searches in the specified vocabulary and all the vocabularies it (transitively) imports.
     *
     * @param searchString String the search term labels by
     * @param vocabulary   Vocabulary whose terms should be searched
     * @return List of matching terms
     */
    public List<Term> findAllIncludingImported(String searchString, Vocabulary vocabulary) {
        Objects.requireNonNull(searchString);
        Objects.requireNonNull(vocabulary);
        final TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "?targetVocabulary ?imports* ?vocabulary ." +
                "?term a ?type ;\n" +
                "      ?hasLabel ?label ;\n" +
                "      ?inVocabulary ?vocabulary ." +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) .\n" +
                "} ORDER BY ?label", Term.class)
                                         .setParameter("type", typeUri)
                                         .setParameter("hasLabel", LABEL_PROP)
                                         .setParameter("inVocabulary", URI.create(
                                                 cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                                         .setParameter("imports",
                                                 URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik))
                                         .setParameter("targetVocabulary", vocabulary.getUri())
                                         .setParameter("searchString", searchString, config.get(ConfigParam.LANGUAGE));
        try {
            final List<Term> terms = executeQueryAndLoadSubTerms(query);
            terms.forEach(this::loadParentSubTerms);
            return terms;
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Checks whether a term with the specified label exists in a vocabulary with the specified URI.
     * <p>
     * Note that this method uses comparison ignoring case, so that two labels differing just in character case are
     * considered same here.
     *
     * @param label      Label to check
     * @param vocabulary Vocabulary in which terms will be searched
     * @return Whether term with {@code label} already exists in vocabulary
     */
    public boolean existsInVocabulary(String label, Vocabulary vocabulary) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(vocabulary);
        try {
            return em.createNativeQuery("ASK { ?term a ?type ; " +
                    "?hasLabel ?label ;" +
                    "?inVocabulary ?vocabulary ." +
                    "FILTER (LCASE(?label) = LCASE(?searchString)) . }", Boolean.class)
                     .setParameter("type", typeUri)
                     .setParameter("hasLabel", LABEL_PROP)
                     .setParameter("inVocabulary",
                             URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                     .setParameter("vocabulary", vocabulary.getUri())
                     .setParameter("searchString", label, config.get(ConfigParam.LANGUAGE)).getSingleResult();
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
