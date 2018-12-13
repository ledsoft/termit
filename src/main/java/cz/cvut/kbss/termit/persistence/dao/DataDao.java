package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDF;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.dto.RdfsResource;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DataDao {

    private static final URI RDFS_LABEL = URI.create(RDFS.LABEL);

    private final EntityManager em;

    private final Configuration config;

    @Autowired
    public DataDao(EntityManager em, Configuration config) {
        this.em = em;
        this.config = config;
    }

    /**
     * Gets all properties present in the system.
     *
     * @return List of properties, ordered by label
     */
    public List<RdfsResource> findAllProperties() {
        return em.createNativeQuery("SELECT ?x ?label ?comment ?type WHERE {" +
                "BIND (?property as ?type)" +
                "?x a ?type ." +
                "OPTIONAL { ?x ?has-label ?label . }" +
                "OPTIONAL { ?x ?has-comment ?comment . }" +
                "}", "RdfsResource")
                 .setParameter("property", URI.create(RDF.PROPERTY))
                 .setParameter("has-label", RDFS_LABEL)
                 .setParameter("has-comment", URI.create(RDFS.COMMENT)).getResultList();
    }

    /**
     * Persists the specified resource.
     * <p>
     * This method should be used very rarely because it saves a basic RDFS resource with nothing but identifier and
     * possibly label and comment.
     *
     * @param instance The resource to persist
     */
    public void persist(RdfsResource instance) {
        Objects.requireNonNull(instance);
        try {
            em.persist(instance);
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Gets basic metadata about a resource with the specified identifier.
     *
     * @param id Resource identifier
     * @return Wrapped matching resource or an empty {@code Optional} if no such resource exists
     */
    public Optional<RdfsResource> find(URI id) {
        Objects.requireNonNull(id);
        final List<RdfsResource> resources = em.createNativeQuery("SELECT ?x ?label ?comment ?type WHERE {" +
                "BIND (?id AS ?x)" +
                "?x a ?type ." +
                "OPTIONAL { ?x ?has-label ?label .}" +
                "OPTIONAL { ?x ?has-comment ?comment . }" +
                "}", "RdfsResource").setParameter("id", id)
                                               .setParameter("has-label", RDFS_LABEL)
                                               .setParameter("has-comment", URI.create(RDFS.COMMENT)).getResultList();
        if (resources.isEmpty()) {
            return Optional.empty();
        }
        final RdfsResource result = resources.get(0);
        result.setTypes(resources.stream().flatMap(r -> r.getTypes().stream()).collect(Collectors.toSet()));
        return Optional.of(result);
    }

    /**
     * Gets the {@link RDFS#LABEL} of a resource with the specified identifier.
     * <p>
     * Note that the label has to have matching language tag or no language tag at all (matching tag is preferred).
     *
     * @param id Resource ({@link RDFS#RESOURCE}) identifier
     * @return Matching resource identifier (if found)
     */
    public Optional<String> getLabel(URI id) {
        Objects.requireNonNull(id);
        try {
            return Optional.of(em.createNativeQuery("SELECT ?label WHERE {" +
                    "?x ?has-label ?label ." +
                    "FILTER (LANGMATCHES(LANG(?label), ?tag) || lang(?label) = \"\") }", String.class)
                                 .setParameter("x", id).setParameter("has-label", RDFS_LABEL)
                                 .setParameter("tag", config.get(ConfigParam.LANGUAGE), null).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
