package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.verify;

class ResourceServiceTest {

    @Mock
    private ResourceRepositoryService resourceRepositoryService;

    @InjectMocks
    private ResourceService sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findAllLoadsResourcesFromRepositoryService() {
        sut.findAll();
        verify(resourceRepositoryService).findAll();
    }

    @Test
    void findLoadsResourceFromRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.find(uri);
        verify(resourceRepositoryService).find(uri);
    }

    @Test
    void findRequiredLoadsResourceFromRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.findRequired(uri);
        verify(resourceRepositoryService).findRequired(uri);
    }

    @Test
    void existsChecksForExistenceViaRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.exists(uri);
        verify(resourceRepositoryService).exists(uri);
    }

    @Test
    void persistSavesResourceViaRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.persist(resource);
        verify(resourceRepositoryService).persist(resource);
    }

    @Test
    void updateUpdatesResourceViaRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.update(resource);
        verify(resourceRepositoryService).update(resource);
    }

    @Test
    void setTagsUpdatesResourceTagsViaRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        final Set<URI> termUris =
                IntStream.range(0, 5).mapToObj(i -> Generator.generateUri()).collect(Collectors.toSet());
        sut.setTags(resource, termUris);
        verify(resourceRepositoryService).setTags(resource, termUris);
    }

    @Test
    void removeRemovesResourceViaRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.remove(resource);
        verify(resourceRepositoryService).remove(resource);
    }

    @Test
    void findTagsLoadsResourceTagsFromRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.findTags(resource);
        verify(resourceRepositoryService).findTags(resource);
    }

    @Test
    void findRelatedLoadsRelatedResourcesFromRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.findRelated(resource);
        verify(resourceRepositoryService).findRelated(resource);
    }
}