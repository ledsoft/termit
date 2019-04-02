/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.XPathSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
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
        resource.setLabel("Metropolitan Plan");
        final User author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
        transactional(() -> {
            em.persist(author);
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
        final Term term = Generator.generateTermWithId();
        transactional(() -> em.persist(term));

        final Target target = new Target();
        target.setSource(resource);
        transactional(() -> em.persist(target));

        final List<TermAssignment> expected = generateAssignmentsForTarget(term, target);
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

    @Test
    void findAllByResourceReturnsAllAssignmentsAndOccurrencesRelatedToSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        final File file = new File();
        file.setLabel("test.html");
        file.setUri(Generator.generateUri());

        final Target target = new Target();
        target.setSource(file);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(target);
            em.persist(file);
        });
        final List<TermAssignment> assignments = generateAssignmentsForTarget(term, target);
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file);

        final List<TermAssignment> result = sut.findAll(file);
        assertEquals(assignments.size() + occurrences.size(), result.size());
    }

    private List<TermOccurrence> generateTermOccurrences(Term term, File file) {
        final List<TermOccurrence> occurrences = new ArrayList<>();
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermOccurrence occurrence = new TermOccurrence(term, new OccurrenceTarget(file));
            // Dummy selector
            occurrence.getTarget().setSelectors(Collections.singleton(new XPathSelector("//div")));
            occurrences.add(occurrence);
        }
        transactional(() -> occurrences.forEach(to -> {
            em.persist(to);
            em.persist(to.getTarget());
        }));
        return occurrences;
    }
}