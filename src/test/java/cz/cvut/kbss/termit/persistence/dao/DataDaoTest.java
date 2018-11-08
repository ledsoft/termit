package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.OWL;
import cz.cvut.kbss.termit.dto.RdfsResource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataDaoTest extends BaseDaoTestRunner {

    private static final String FIRST_NAME_LABEL = "First name";

    @Autowired
    private EntityManager em;

    @Autowired
    private DataDao sut;

    @Test
    void findAllPropertiesGetsPropertiesFromRepository() {
        generateProperties();
        final List<RdfsResource> result = sut.findAllProperties();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(r -> r.getUri().equals(URI.create(Vocabulary.s_p_ma_krestni_jmeno))));
        assertTrue(result.stream().anyMatch(r -> r.getUri().equals(URI.create(Vocabulary.s_p_ma_prijmeni))));
        assertTrue(result.stream().anyMatch(r -> r.getUri().equals(URI.create(Vocabulary.s_p_ma_uzivatelske_jmeno))));
    }

    private void generateProperties() {
        // Here we are simulating schema presence in the repository
        transactional(() -> {
            final Repository repo = em.unwrap(Repository.class);
            final ValueFactory vf = repo.getValueFactory();
            try (final RepositoryConnection connection = repo.getConnection()) {
                connection.add(vf.createIRI(OWL.DATATYPE_PROPERTY), RDFS.SUBCLASSOF, RDF.PROPERTY);
                connection.add(vf.createIRI(OWL.OBJECT_PROPERTY), RDFS.SUBCLASSOF, RDF.PROPERTY);
                connection.add(vf.createIRI(OWL.ANNOTATION_PROPERTY), RDFS.SUBCLASSOF, RDF.PROPERTY);
                connection.add(vf.createIRI(Vocabulary.s_p_ma_krestni_jmeno), RDF.TYPE,
                        vf.createIRI(OWL.DATATYPE_PROPERTY));
                connection.add(vf.createIRI(Vocabulary.s_p_ma_krestni_jmeno), RDFS.LABEL,
                        vf.createLiteral(FIRST_NAME_LABEL));
                connection.add(vf.createIRI(Vocabulary.s_p_ma_prijmeni), RDF.TYPE, vf.createIRI(OWL.DATATYPE_PROPERTY));
                connection.add(vf.createIRI(Vocabulary.s_p_ma_uzivatelske_jmeno), RDF.TYPE,
                        vf.createIRI(OWL.DATATYPE_PROPERTY));
                connection.commit();
            }
        });
    }

    @Test
    void findReturnsMatchingResource() {
        generateProperties();
        final Optional<RdfsResource> result = sut.find(URI.create(Vocabulary.s_p_ma_krestni_jmeno));
        assertTrue(result.isPresent());
        assertEquals(Vocabulary.s_p_ma_krestni_jmeno, result.get().getUri().toString());
        assertEquals(FIRST_NAME_LABEL, result.get().getLabel());
    }

    @Test
    void findReturnsEmptyOptionalWhenNoMatchingResourceIsFound() {
        generateProperties();
        final Optional<RdfsResource> result = sut.find(URI.create(Vocabulary.s_c_omezeny_uzivatel_termitu));
        assertFalse(result.isPresent());
    }
}