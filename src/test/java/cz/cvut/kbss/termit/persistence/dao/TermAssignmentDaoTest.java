package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermAssignments;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermOccurrences;
import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.dto.assignment.TermOccurrences;
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
    void getAssignmentsInfoByTermRetrievesAssignmentsOfSpecifiedTerm() {
        final Term term = Generator.generateTermWithId();
        final File file = Generator.generateFileWithId("test.html");
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(file);
        });
        generateAssignment(term, resource, false);
        generateAssignment(term, file, false);

        final List<TermAssignments> result = sut.getAssignmentInfo(term);
        assertEquals(2, result.size());
        for (TermAssignments tai : result) {
            if (tai.getResource().equals(resource.getUri())) {
                assertEquals(resource.getLabel(), tai.getResourceLabel());
            } else {
                assertEquals(file.getLabel(), tai.getResourceLabel());
            }
            assertEquals(term.getUri(), tai.getTerm());
        }
    }

    private void generateAssignment(Term term, Resource resource, boolean suggested) {
        final Target target = new Target();
        target.setSource(resource.getUri());
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(term.getUri());
        ta.setTarget(target);
        if (suggested) {
            ta.addType(Vocabulary.s_c_navrzene_prirazeni_termu);
        }
        transactional(() -> {
            em.persist(ta);
            em.persist(ta.getTarget());
        });
    }

    @Test
    void getAssignmentsInfoByTermRetrievesAggregateTermOccurrences() {
        final Term term = Generator.generateTermWithId();
        final File fOne = Generator.generateFileWithId("testOne.html");
        final File fTwo = Generator.generateFileWithId("testTwo.html");
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(fOne);
            em.persist(fTwo);
        });
        final List<TermOccurrence> occurrencesOne = generateTermOccurrences(term, fOne, false);
        final List<TermOccurrence> occurrencesTwo = generateTermOccurrences(term, fTwo, true);

        final List<TermAssignments> result = sut.getAssignmentInfo(term);
        assertEquals(2, result.size());
        for (TermAssignments tai : result) {
            assertTrue(tai instanceof TermOccurrences);
            final TermOccurrences toi = (TermOccurrences) tai;
            if (toi.getResource().equals(fOne.getUri())) {
                assertEquals(occurrencesOne.size(), toi.getCount().intValue());
            } else {
                assertEquals(occurrencesTwo.size(), toi.getCount().intValue());
            }
        }
    }

    @Test
    void getAssignmentsInfoByTermRetrievesSeparateInstancesForSuggestedAndAssertedOccurrencesOfSameTerm() {
        final Term term = Generator.generateTermWithId();
        final File file = Generator.generateFileWithId("testOne.html");
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(file);
        });
        generateTermOccurrences(term, file, false);
        generateTermOccurrences(term, file, true);

        final List<TermAssignments> result = sut.getAssignmentInfo(term);
        assertEquals(2, result.size());
        result.forEach(tai -> assertEquals(term.getUri(), tai.getTerm()));
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
    void getAssignmentsInfoByResourceRetrievesInformationAboutAssignmentsAndOccurrencesForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");

        generateAssignment(term, file, false);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(file);
        });
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file, false);

        final List<ResourceTermAssignments> result = sut.getAssignmentInfo(file);
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
    void getAssignmentsInfoByResourceRetrievesInfoAboutSuggestedAssignmentsAndOccurrencesForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");

        generateAssignment(term, file, true);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(file);
        });
        final List<TermOccurrence> occurrences = generateTermOccurrences(term, file, true);

        final List<ResourceTermAssignments> result = sut.getAssignmentInfo(file);
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
    void getAssignmentsInfoByResourceRetrievesAssignmentsWhenNoOccurrencesAreFoundForSpecifiedResource() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());

        generateAssignment(term, resource, true);
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
        });

        final List<ResourceTermAssignments> result = sut.getAssignmentInfo(resource);
        assertEquals(1, result.size());
        assertEquals(resource.getUri(), result.get(0).getResource());
        assertEquals(term.getUri(), result.get(0).getTerm());
        assertEquals(term.getLabel(), result.get(0).getTermLabel());
    }

    @Test
    void getAssignmentsInfoByResourceRetrievesSeparateInstancesForSuggestedAndAssertedOccurrencesOfSameTerm() {
        final Term term = Generator.generateTermWithId();
        term.setVocabulary(Generator.generateUri());
        final File file = Generator.generateFileWithId("test.html");
        transactional(() -> {
            enableRdfsInference(em);
            em.persist(term);
            em.persist(file);
        });
        final List<TermOccurrence> suggested = generateTermOccurrences(term, file, true);
        final List<TermOccurrence> asserted = generateTermOccurrences(term, file, false);

        final List<ResourceTermAssignments> result = sut.getAssignmentInfo(file);
        assertEquals(2, result.size());
        for (ResourceTermAssignments rta : result) {
            if (rta.getTypes().contains(Vocabulary.s_c_navrzeny_vyskyt_termu)) {
                assertEquals(suggested.size(), ((ResourceTermOccurrences) rta).getCount().intValue());
            } else {
                assertEquals(asserted.size(), ((ResourceTermOccurrences) rta).getCount().intValue());
            }
            assertEquals(term.getUri(), rta.getTerm());
            assertEquals(term.getLabel(), rta.getTermLabel());
            assertEquals(file.getUri(), rta.getResource());
            assertEquals(term.getVocabulary(), rta.getVocabulary());
        }
    }
}