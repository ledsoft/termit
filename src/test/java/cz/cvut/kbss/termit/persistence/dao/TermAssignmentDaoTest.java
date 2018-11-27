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
            ta.setTarget(new Target(resource));
            assignments.add(ta);
        }
        transactional(() -> assignments.forEach(ta -> {
            em.persist(ta.getTarget());
            em.persist(ta);
        }));
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

    private List<TermAssignment> generateAssignmentsForTarget(Term term, Target target) {
        final List<TermAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermAssignment ta = new TermAssignment();
            ta.setTerm(term);
            ta.setTarget(target);
            assignments.add(ta);
        }
        transactional(() -> assignments.forEach(em::persist));
        return assignments;
    }

    @Test
    void findByTargetReturnsCorrectAssignments() {
        final Term term = new Term();
        term.setLabel("TestTerm");
        term.setUri(Generator.generateUri());
        transactional(() -> em.persist(term));

        final Target target = new Target();
        target.setSource(resource);
        transactional(() -> em.persist(target));

        final List<TermAssignment> expected = generateAssignmentsForTarget(term,target);
        final List<TermAssignment> result = sut.findByTarget(target);
        assertEquals(expected.size(), result.size());
        for (TermAssignment ta : result) {
            assertTrue(expected.stream().anyMatch(assignment -> assignment.getUri().equals(ta.getUri())));
        }
    }

    @Test
    void findAllByTargetReturnsNoAssignmentsForTargetWithoutAssignment() {
        final Target target = new Target();
        target.setSource(resource);
        target.setUri(Generator.generateUri());
        transactional(() -> em.persist(target));

        assertTrue(sut.findByTarget(target).isEmpty());
    }
}