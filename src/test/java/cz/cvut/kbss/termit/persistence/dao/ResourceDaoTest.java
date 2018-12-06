package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDaoTest extends BaseDaoTestRunner {

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
    void findTermsReturnsTermsWhichAreAssignedToSpecifiedResource() {
        final Resource resource = generateResource();
        final List<Term> terms = generateTerms(resource);

        final List<Term> result = sut.findTerms(resource);
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
    }

    private Resource generateResource() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setAuthor(user);
        resource.setDateCreated(new Date());
        transactional(() -> em.persist(resource));
        return resource;
    }

    private List<Term> generateTerms(Resource resource) {
        final List<Term> terms = new ArrayList<>();
        final List<Term> matching = new ArrayList<>();
        final List<TermAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < Generator.randomInt(2, 10); i++) {
            final Term t = Generator.generateTermWithId();
            terms.add(t);
            if (Generator.randomBoolean() || matching.isEmpty()) {
                matching.add(t);
                final TermAssignment ta = new TermAssignment();
                ta.setTerm(t);
                ta.setTarget(new Target(resource));
                assignments.add(ta);
            }
        }
        transactional(() -> {
            terms.forEach(em::persist);
            assignments.forEach(ta -> {
                em.persist(ta.getTarget());
                em.persist(ta);
            });
        });
        return matching;
    }

    @Test
    void findRelatedReturnsResourcesWhichHaveSameTermsAssigned() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> generateResource())
                                                  .collect(Collectors.toList());
        final Resource resource = resources.get(Generator.randomIndex(resources));
        final List<Resource> related = resources.stream().filter(r -> !r.equals(resource) && Generator.randomBoolean())
                                                .collect(
                                                        Collectors.toList());
        generateCommonTerms(resource, related);

        final List<Resource> result = sut.findRelated(resource);
        assertEquals(related.size(), result.size());
        assertTrue(related.containsAll(result));
    }

    private void generateCommonTerms(Resource resource, List<Resource> related) {
        final List<Term> terms = generateTerms(resource);
        final List<TermAssignment> assignments = new ArrayList<>();
        for (Resource res : related) {
            final Term common = terms.get(Generator.randomIndex(terms));
            final TermAssignment ta = new TermAssignment();
            ta.setTerm(common);
            ta.setTarget(new Target(res));
            assignments.add(ta);
        }
        transactional(() -> assignments.forEach(ta -> {
            em.persist(ta.getTarget());
            em.persist(ta);
        }));
    }
}