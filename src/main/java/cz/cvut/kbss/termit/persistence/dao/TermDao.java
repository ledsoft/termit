package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.ObjectPropertyCollectionDescriptor;
import cz.cvut.kbss.jopa.model.query.TypedQuery;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermDao extends AssetDao<Term> {

    private final Configuration config;

    @Autowired
    public TermDao(EntityManager em, Configuration config) {
        super(Term.class, em);
        this.config = config;
    }

    /**
     * Persists the specified term into the specified vocabulary's context.
     * <p>
     * Note that this is the preferred way of persisting terms.
     *
     * @param instance   The instance to persist
     * @param vocabulary Vocabulary to which the instance belongs
     */
    public void persist(Term instance, Vocabulary vocabulary) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(vocabulary);
        try {
            em.persist(instance, DescriptorFactory.termDescriptor(vocabulary));
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
            return em.merge(entity, DescriptorFactory.termDescriptor(entity.getVocabulary()));
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
        return em.createNativeQuery("SELECT DISTINCT ?term WHERE {" +
                "?term a ?type ;" +
                "rdfs:label ?label ;" +
                "?inVocabulary ?vocabulary ." +
                "FILTER (lang(?label) = ?labelLang) ." +
                "} ORDER BY ?label", Term.class)
                 .setParameter("type", typeUri)
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setParameter("inVocabulary",
                         URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                 .getResultList();
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
                "?term a ?type ;" +
                "rdfs:label ?label ." +
                "?vocabulary ?hasGlossary/?hasTerm ?term ." +
                "FILTER (lang(?label) = ?labelLang) ." +
                "} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class);
        query = setCommonFindAllRootsQueryParams(query, false);
        return query.setParameter("vocabulary", vocabulary.getUri())
                    .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                    .setUntypedParameter("offset", pageSpec.getOffset())
                    .setUntypedParameter("limit", pageSpec.getPageSize())
                    .getResultList();
    }

    private <T> TypedQuery<T> setCommonFindAllRootsQueryParams(TypedQuery<T> query, boolean includeImports) {
        final TypedQuery<T> tq = query.setParameter("type", typeUri)
                                      .setParameter("hasGlossary",
                                              URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar))
                                      .setParameter("hasTerm", URI.create(
                                              cz.cvut.kbss.termit.util.Vocabulary.s_p_obsahuje_korenovy_pojem));
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
                "rdfs:label ?label ." +
                "?vocabulary ?imports* ?parent ." +
                "?parent ?hasGlossary/?hasTerm ?term ." +
                "FILTER (lang(?label) = ?labelLang) ." +
                "} ORDER BY ?label OFFSET ?offset LIMIT ?limit", Term.class);
        query = setCommonFindAllRootsQueryParams(query, true);
        return query.setParameter("vocabulary", vocabulary.getUri())
                    .setParameter("labelLang", config.get(ConfigParam.LANGUAGE))
                    .setUntypedParameter("offset", pageSpec.getOffset())
                    .setUntypedParameter("limit", pageSpec.getPageSize())
                    .getResultList();
    }

    /**
     * Finds root terms whose term subtree contains a term with label matching the specified search string.
     * <p>
     * Currently, the match uses SPARQL {@code contains} function on lowercase label and search string. A more
     * sophisticated matching can be added.
     *
     * @param searchString String to search term labels by
     * @param vocabulary   Vocabulary whose terms should be returned
     * @return List of root terms contain a matching term in subtree
     */
    public List<Term> findAllRoots(String searchString, Vocabulary vocabulary) {
        Objects.requireNonNull(searchString);
        Objects.requireNonNull(vocabulary);
        TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?root WHERE {" +
                "?root a ?type ." +
                "?vocabulary ?hasGlossary/?hasTerm ?root ." +
                "?root ?hasChild* ?term ." +
                "{\n ?term a ?type ;" +
                "rdfs:label ?label ." +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) ." +
                "}\n} ORDER BY ?label", Term.class);
        query = setCommonFindAllRootsQueryParams(query, false);
        return query.setParameter("vocabulary", vocabulary.getUri())
                    .setParameter("hasChild", URI.create(SKOS.NARROWER))
                    .setParameter("searchString", searchString, config.get(ConfigParam.LANGUAGE))
                    .getResultList();
    }

    /**
     * Finds root terms whose term subtree contains a term with label matching the specified search string.
     * <p>
     * The specified vocabulary represents the starting point of a transitive closure over the vocabulary import
     * relationship. Terms from all vocabularies reached via this closure are taken into account in the search, so the
     * returned root terms need not be in the specified vocabulary.
     * <p>
     * Currently, the match uses SPARQL {@code contains} function on lowercase label and search string. A more
     * sophisticated matching can be added.
     *
     * @param searchString String to search term labels by
     * @param vocabulary   Vocabulary whose terms should be returned
     * @return List of root terms contain a matching term in subtree
     */
    public List<Term> findAllRootsIncludingImports(String searchString, Vocabulary vocabulary) {
        Objects.requireNonNull(searchString);
        Objects.requireNonNull(vocabulary);
        TypedQuery<Term> query = em.createNativeQuery("SELECT DISTINCT ?root WHERE {" +
                "?root a ?type ." +
                "?vocabulary ?imports* ?parent ." +
                "?parent ?hasGlossary/?hasTerm ?root." +
                "?root ?hasChild* ?term ." +
                "{\n ?term a ?type ;" +
                "rdfs:label ?label ." +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) ." +
                "}\n} ORDER BY ?label", Term.class);
        query = setCommonFindAllRootsQueryParams(query, true);
        return query.setParameter("vocabulary", vocabulary.getUri())
                    .setParameter("hasChild", URI.create(SKOS.NARROWER))
                    .setParameter("searchString", searchString, config.get(ConfigParam.LANGUAGE))
                    .getResultList();
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
        return em.createNativeQuery("ASK { ?term a ?type ; " +
                "?hasLabel ?label ;" +
                "?inVocabulary ?vocabulary ." +
                "FILTER (LCASE(?label) = LCASE(?searchString)) . }", Boolean.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasLabel", URI.create(RDFS.LABEL))
                 .setParameter("inVocabulary",
                         URI.create(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku))
                 .setParameter("vocabulary", vocabulary.getUri())
                 .setParameter("searchString", label, config.get(ConfigParam.LANGUAGE)).getSingleResult();
    }
}