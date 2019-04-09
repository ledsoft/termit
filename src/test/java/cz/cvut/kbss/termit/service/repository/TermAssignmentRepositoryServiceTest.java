package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TermAssignmentRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermAssignmentRepositoryService sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findAllRetrievesAssignmentsForSpecifiedResource() {
        final Resource resource = Generator.generateResourceWithId();
        final Target target = new Target(resource);
        final Term term = Generator.generateTermWithId();
        final TermAssignment ta = new TermAssignment(term, target);
        transactional(() -> {
            em.persist(target);
            em.persist(resource);
            em.persist(term);
            em.persist(ta);
        });

        final List<TermAssignment> result = sut.findAll(resource);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ta.getUri(), result.get(0).getUri());
    }

    @Test
    void removeAllDeletesAssignmentsOfSpecifiedResource() {
        final Resource resource = Generator.generateResourceWithId();
        final Target target = new Target(resource);
        final Term term = Generator.generateTermWithId();
        final TermAssignment ta = new TermAssignment(term, target);
        transactional(() -> {
            em.persist(target);
            em.persist(resource);
            em.persist(term);
            em.persist(ta);
        });

        sut.removeAll(resource);
        verifyInstancesRemoved(Vocabulary.s_c_prirazeni_termu, em);
        verifyInstancesRemoved(Vocabulary.s_c_cil, em);
    }

    @Test
    void setInvalidTagsForValidResource() {
        assertThrows(NotFoundException.class, () -> {
            final Resource resource = generateResource();
            final Set<URI> terms = new HashSet<>();
            terms.add(URI.create("http://unknown.uri/term1"));
            terms.add(URI.create("http://unknown.uri/term2"));
            transactional(() -> sut.setOnResource(resource, terms));
        });
    }

    private Resource generateResource() {
        final Resource resource = Generator.generateResourceWithId();
        transactional(() -> em.persist(resource));
        return resource;
    }

    @Test
    void addTagsToUntaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setOnResource(resource, tags));

        final List<TermAssignment> result = sut.findAll(resource);
        assertEquals(2, result.size());
        assertEquals(tags, result.stream().map(ta -> ta.getTerm().getUri()).collect(Collectors.toSet()));
    }

    private Term generateTermWithUriAndPersist() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        transactional(() -> em.persist(t));
        return t;
    }

    @Test
    void replaceTagsOfTaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setOnResource(resource, tags));

        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        final URI term3 = generateTermWithUriAndPersist().getUri();
        tags2.add(term2);
        tags2.add(term3);
        transactional(() -> sut.setOnResource(resource, tags2));

        final List<TermAssignment> result = sut.findAll(resource);
        assertEquals(2, result.size());
        assertEquals(tags2, result.stream().map(ta -> ta.getTerm().getUri()).collect(Collectors.toSet()));
    }

    @Test
    void setTagsMergesExistingTagsAndNewlySpecifiedTags() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setOnResource(resource, tags));
        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        tags2.add(term0);
        tags2.add(term2);

        transactional(() -> sut.setOnResource(resource, tags2));

        final List<TermAssignment> result = sut.findAll(resource);
        assertEquals(2, result.size());
        assertEquals(tags2, result.stream().map(ta -> ta.getTerm().getUri()).collect(Collectors.toSet()));
    }
}