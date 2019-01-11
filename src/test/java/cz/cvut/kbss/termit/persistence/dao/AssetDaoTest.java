package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findRecentlyEditedLoadsSpecifiedCountOfRecentlyEditedResources() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<Resource> old = resources.subList(0, 5);
        final List<Resource> recent = resources.subList(5, resources.size());
        // We are setting the date here to work around the ProvenanceManager, which sets creation date on persist automatically
        transactional(() -> old.forEach(res -> {
            res.setCreated(new Date(System.currentTimeMillis() - 100000));
            em.merge(res);
        }));

        final int count = 3;
        final List<Resource> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result));
    }

    @Test
    void findRecentlyEditedUsesLastModifiedDateWhenAvailable() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<Resource> recent = resources.subList(5, resources.size());
        transactional(() -> {
            setOldCreated(resources);
            recent.forEach(r -> {
                r.setDescription("Update");
                r.setLastModified(new Date());
                em.merge(r);
            });
        });
        em.getEntityManagerFactory().getCache().evictAll();

        final int count = 3;
        final List<Resource> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result));
    }

    private void setOldCreated(List<Resource> old) {
        final Repository repo = em.unwrap(Repository.class);
        final ValueFactory vf = repo.getValueFactory();
        try (final RepositoryConnection con = repo.getConnection()) {
            con.begin();
            old.forEach(r -> {
                con.remove(vf.createIRI(r.getUri().toString()), vf.createIRI(Vocabulary.s_p_created), null);
                con.add(vf.createIRI(r.getUri().toString()), vf.createIRI(Vocabulary.s_p_created),
                        vf.createLiteral(new Date(System.currentTimeMillis() - 24 * 3600 * 1000)));
            });
            con.commit();
        }
    }
}