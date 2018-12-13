package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
        Environment.setCurrentUser(user);
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

    private Term generateTermWithUriAndPersist() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        transactional(() -> em.persist(t));
        return t;
    }

    @Test
    void setTagsForInvalidResource() {
        assertThrows(NotFoundException.class, () -> {
            final Term term1 = generateTermWithUriAndPersist();
            final Term term2 = generateTermWithUriAndPersist();

            final Set<Term> terms = new HashSet<>();
            terms.add(term1);
            terms.add(term2);
            transactional(() -> sut.setTags(URI.create("http://unknown.uri/resource"),
                    terms.stream().map(Term::getUri).collect(Collectors.toSet())));
        });
    }

    @Test
    void setInvalidTagsForValidResource() {
        assertThrows(NotFoundException.class, () -> {
            final Resource resource = generateResource();
            final Set<URI> terms = new HashSet<>();
            terms.add(URI.create("http://unknown.uri/term1"));
            terms.add(URI.create("http://unknown.uri/term2"));
            transactional(() -> sut.setTags(resource.getUri(), terms));
        });
    }

    @Test
    void addTagsToUntaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource.getUri(), tags));

        assertEquals(2, sut.findTerms(resource).size());
        assertEquals(tags, sut.findTerms(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void replaceTagsOfTaggedResource() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource.getUri(), tags));

        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        final URI term3 = generateTermWithUriAndPersist().getUri();
        tags2.add(term2);
        tags2.add(term3);
        transactional(() -> sut.setTags(resource.getUri(), tags2));

        assertEquals(2, sut.findTerms(resource).size());
        assertEquals(tags2, sut.findTerms(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void setTagsMergesExistingTagsAndNewlySpecifiedTags() {
        final Resource resource = generateResource();

        final Set<URI> tags = new HashSet<>();
        final URI term0 = generateTermWithUriAndPersist().getUri();
        final URI term1 = generateTermWithUriAndPersist().getUri();
        tags.add(term0);
        tags.add(term1);

        transactional(() -> sut.setTags(resource.getUri(), tags));
        final Set<URI> tags2 = new HashSet<>();
        final URI term2 = generateTermWithUriAndPersist().getUri();
        tags2.add(term0);
        tags2.add(term2);

        transactional(() -> sut.setTags(resource.getUri(), tags2));

        assertEquals(2, sut.findTerms(resource).size());
        assertEquals(tags2, sut.findTerms(resource).stream().map(Term::getUri).collect(Collectors.toSet()));
    }

    @Test
    void persistThrowsValidationExceptionWhenResourceLabelIsMissing() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setName(null);
        assertThrows(ValidationException.class, () -> sut.persist(resource));
    }
}