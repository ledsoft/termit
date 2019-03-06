package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.XPathSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
        resource.setCreated(new Date());
        transactional(() -> em.persist(resource));
        return resource;
    }

    private List<Term> generateTerms(Resource resource) {
        final List<Term> terms = new ArrayList<>();
        final List<Term> matching = new ArrayList<>();
        final List<TermAssignment> assignments = new ArrayList<>();
        final Target target = new Target(resource);
        for (int i = 0; i < Generator.randomInt(2, 10); i++) {
            final Term t = Generator.generateTermWithId();
            terms.add(t);
            if (Generator.randomBoolean() || matching.isEmpty()) {
                matching.add(t);
                final TermAssignment ta = new TermAssignment();
                ta.setTerm(t);
                ta.setTarget(target);
                assignments.add(ta);
            }
        }
        transactional(() -> {
            terms.forEach(em::persist);
            assignments.forEach(ta -> {
                em.persist(target);
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

    @Test
    void findAllDoesNotReturnFilesContainedInDocuments() {
        enableRdfsInference(em);
        final Resource rOne = Generator.generateResourceWithId();
        final Document doc = new Document();
        doc.setUri(Generator.generateUri());
        doc.setLabel("document");
        final File file = new File();
        file.setUri(Generator.generateUri());
        file.setLabel("mpp.html");
        doc.addFile(file);
        transactional(() -> {
            em.persist(rOne);
            em.persist(doc);
            em.persist(file);
        });

        final List<Resource> result = sut.findAll();
        assertEquals(2, result.size());
        assertFalse(result.contains(file));
        assertTrue(result.contains(doc));
        final Optional<Resource> docResult = result.stream().filter(r -> r.getUri().equals(doc.getUri())).findAny();
        assertTrue(docResult.isPresent());
        assertTrue(((Document) docResult.get()).getFile(file.getLabel()).isPresent());
    }

    @Test
    void findTermsReturnsDistinctTermsInCaseSomeOccurMultipleTimesInResource() {
        final File resource = new File();
        resource.setLabel("test.html");
        resource.setUri(Generator.generateUri());
        transactional(() -> em.persist(resource));
        final List<Term> terms = generateTerms(resource);
        generateOccurrences(resource, terms);
        final List<Term> result = sut.findTerms(resource);
        final Set<Term> resultSet = new HashSet<>(result);
        assertEquals(result.size(), resultSet.size());
    }

    private void generateOccurrences(File resource, List<Term> terms) {
        final List<TermOccurrence> occurrences = new ArrayList<>();
        for (Term t : terms) {
            final TermOccurrence occurrence = new TermOccurrence(t, new OccurrenceTarget(resource));
            // Dummy selector
            occurrence.getTarget().setSelectors(Collections.singleton(new XPathSelector("//div")));
            occurrences.add(occurrence);
        }
        transactional(() -> occurrences.forEach(occ -> {
            em.persist(occ);
            em.persist(occ.getTarget());
        }));
    }
}