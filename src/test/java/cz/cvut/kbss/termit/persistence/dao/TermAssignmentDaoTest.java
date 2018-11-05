package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermAssignmentDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermAssignmentDao sut;

    private Resource resource;

    @BeforeEach
    void setUp() {
        this.resource = new Resource();
        resource.setUri(Generator.generateUri());
        resource.setName("Metropolitan Plan");
        resource.setAuthor(Generator.generateUserWithId());
        resource.setDateCreated(new Date());
        transactional(() -> {
            em.persist(resource.getAuthor());
            em.persist(resource);
        });
    }

    @Test
    void findAllByTermReturnsAssignmentsOfGivenTerm() {
        final Term term = new Term();
        term.setLabel("TestTerm");
        term.setUri(Generator.generateUri());
        transactional(() -> em.persist(term));

        final List<TermAssignment> expected = generateAssignments(term);
        final List<TermAssignment> result = sut.findAll(term);
        assertEquals(expected.size(), result.size());
        for (TermAssignment ta : result) {
            assertTrue(expected.stream().anyMatch(assignment -> assignment.getUri().equals(ta.getUri())));
        }
    }

    private List<TermAssignment> generateAssignments(Term term) {
        final List<TermAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermAssignment ta = new TermAssignment();
            ta.setTerm(term);
            final Target target = new Target(resource);
            ta.setTarget(target);
            assignments.add(ta);
        }
        transactional(() -> assignments.forEach(em::persist));
        return assignments;
    }

    @Test
    void findAllByTermReturnsEmptyCollectionForTermWithoutAssignments() {
        final Term term = new Term();
        term.setLabel("TestTerm");
        term.setUri(Generator.generateUri());
        transactional(() -> em.persist(term));

        assertTrue(sut.findAll(term).isEmpty());
    }
}