package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermAssignments;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermOccurrences;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.XPathSelector;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
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
            ta.setTerm(term.getUri());
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
            ta.setTerm(term.getUri());
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
        target.setSource(resource.getUri());
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
        target.setSource(resource.getUri());
        target.setUri(Generator.generateUri());
        transactional(() -> em.persist(target));

        assertTrue(sut.findByTarget(target).isEmpty());
    }

    @Test
    void findAllByResourceReturnsAllAssignmentsAndOccurrencesRelatedToSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        final File file = Generator.generateFileWithId("test.html");

        final Target target = new Target();
        target.setSource(file.getUri());
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(target);
            em.persist(file);
        });
        final List<TermAssignment> assignments = generateAssignmentsForTarget(term, target);
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file, false);

        final List<TermAssignment> result = sut.findAll(file);
        assertEquals(assignments.size() + occurrences.size(), result.size());
    }

    private List<TermOccurrence> generateTermOccurrences(Term term, File file, boolean suggested) {
        final List<TermOccurrence> occurrences = new ArrayList<>();
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermOccurrence occurrence = new TermOccurrence(term.getUri(), new OccurrenceTarget(file));
            if (suggested) {
                occurrence.addType(Vocabulary.s_c_navrzeny_vyskyt_termu);
            }
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

    @Test
    void getAssignmentsInfoRetrievesInformationAboutAssignmentsAndOccurrencesForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");

        final Target target = new Target();
        target.setSource(file.getUri());
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(term.getUri());
        ta.setTarget(target);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(target);
            em.persist(ta);
            em.persist(file);
        });
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file, false);

        final List<ResourceTermAssignments> result = sut.getAssignmentsInfo(file);
        assertEquals(2, result.size());
        result.forEach(rta -> {
            assertEquals(file.getUri(), rta.getResource());
            assertEquals(term.getUri(), rta.getTerm());
            assertEquals(term.getLabel(), rta.getTermLabel());
        });
        final Optional<ResourceTermAssignments> occ = result.stream()
                                                            .filter(rta -> rta instanceof ResourceTermOccurrences)
                                                            .findAny();
        assertTrue(occ.isPresent());
        assertEquals(occurrences.size(), ((ResourceTermOccurrences) occ.get()).getCount().intValue());
    }

    @Test
    void getAssignmentsInfoRetrievesInfoAboutSuggestedAssignmentsAndOccurrencesForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");

        final Target target = new Target();
        target.setSource(file.getUri());
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(term.getUri());
        ta.setTarget(target);
        ta.addType(Vocabulary.s_c_navrzene_prirazeni_termu);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(target);
            em.persist(ta);
            em.persist(file);
        });
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file, true);

        final List<ResourceTermAssignments> result = sut.getAssignmentsInfo(file);
        assertEquals(2, result.size());
        result.forEach(rta -> {
            assertEquals(file.getUri(), rta.getResource());
            assertEquals(term.getUri(), rta.getTerm());
            assertEquals(term.getLabel(), rta.getTermLabel());
            assertThat(rta.getTypes(), anyOf(hasItem(Vocabulary.s_c_navrzene_prirazeni_termu),
                    hasItem(Vocabulary.s_c_navrzeny_vyskyt_termu)));
        });
        final Optional<ResourceTermAssignments> occ = result.stream()
                                                            .filter(rta -> rta instanceof ResourceTermOccurrences)
                                                            .findAny();
        assertTrue(occ.isPresent());
        assertEquals(occurrences.size(), ((ResourceTermOccurrences) occ.get()).getCount().intValue());
    }

    @Test
    void getAssignmentsInfoRetrievesAssignmentsWhenNoOccurrencesAreFoundForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");

        final Target target = new Target();
        target.setSource(file.getUri());
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(term.getUri());
        ta.setTarget(target);
        ta.addType(Vocabulary.s_c_navrzene_prirazeni_termu);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(target);
            em.persist(ta);
            em.persist(file);
        });

        final List<ResourceTermAssignments> result = sut.getAssignmentsInfo(file);
        assertEquals(1, result.size());
        assertEquals(file.getUri(), result.get(0).getResource());
        assertEquals(term.getUri(), result.get(0).getTerm());
        assertEquals(term.getLabel(), result.get(0).getTermLabel());
    }
}