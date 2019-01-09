package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TargetDao sut;

    private Resource resource;

    @BeforeEach
    void setUp() {
        this.resource = new Resource();
        resource.setUri(Generator.generateUri());
        resource.setLabel("Metropolitan Plan");
        final User author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        resource.setDateCreated(new Date());
        transactional(() -> {
            em.persist(author);
            em.persist(resource);
        });
    }

    @Test
    void findByWholeResourceReturnsAssignmentsOfGivenResource() {
        final Target target = new Target(resource);
        transactional(() -> em.persist(target));
        final Optional<Target> result = sut.findByWholeResource(resource);
        assertEquals(target.getUri(), result.get().getUri());
    }

    @Test
    void findByWholeResourceReturnsEmptyCollectionForResourceWithoutTarget() {
        assertFalse(sut.findByWholeResource(resource).isPresent());
    }
}