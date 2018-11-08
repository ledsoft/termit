package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.dto.RdfsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DataDao {

    private final EntityManager em;

    @Autowired
    public DataDao(EntityManager em) {
        this.em = em;
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
                 // TODO Replace with RDF.PROPERTY once the new JOPA release which fixes the incorrect IRI is out
                 .setParameter("property", URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"))
                 .setParameter("has-label", URI.create(RDFS.LABEL))
                 .setParameter("has-comment", URI.create(RDFS.COMMENT)).getResultList();
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
                                               .setParameter("has-label", URI.create(RDFS.LABEL))
                                               .setParameter("has-comment", URI.create(RDFS.COMMENT)).getResultList();
        if (resources.isEmpty()) {
            return Optional.empty();
        }
        final RdfsResource result = resources.get(0);
        result.setTypes(resources.stream().flatMap(r -> r.getTypes().stream()).collect(Collectors.toSet()));
        return Optional.of(result);
    }
}
