package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
            res.setDateCreated(new Date(System.currentTimeMillis() - 100000));
            em.merge(res);
        }));


        final int count = 3;
        final List<Resource> result = sut.findRecentlyEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result));
    }
}