package cz.cvut.kbss.termit.service.jmx;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppAdminBeanTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private EntityManager em;

    private AppAdminBean sut;

    @BeforeEach
    void setUp() {
        this.sut = new AppAdminBean(emf);
    }

    @Test
    void invalidateCachesClearsPersistenceSecondLevelCache() {
        final User entity = Generator.generateUserWithId();
        transactional(() -> em.persist(entity));
        assertTrue(emf.getCache().contains(User.class, entity.getUri(), new EntityDescriptor()));
        sut.invalidateCaches();
        assertFalse(emf.getCache().contains(User.class, entity.getUri(), new EntityDescriptor()));
    }
}