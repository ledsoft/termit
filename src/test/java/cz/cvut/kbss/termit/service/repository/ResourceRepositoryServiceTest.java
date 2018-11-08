package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceRepositoryService sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
    }

    @Test
    void findTermsReturnsEmptyListWhenNoTermsAreFoundForResource() {
        final Resource resource = generateResource();

        final List<Term> result = sut.findTerms(resource);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private Resource generateResource() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setAuthor(user);
        resource.setDateCreated(new Date());
        transactional(() -> em.persist(resource));
        return resource;
    }

    @Test
    void findRelatedReturnsEmptyListWhenNoRelatedResourcesAreFoundForResource() {
        final Resource resource = generateResource();

        final List<Resource> result = sut.findRelated(resource);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}