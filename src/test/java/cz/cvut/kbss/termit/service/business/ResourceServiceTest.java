package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.UnsupportedAssetOperationException;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.document.DocumentManager;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ResourceServiceTest {

    @Mock
    private ResourceRepositoryService resourceRepositoryService;

    @Mock
    private DocumentManager documentManager;

    @Mock
    private TextAnalysisService textAnalysisService;

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

    @Test
    void getContentLoadsContentOfFileFromDocumentManager() {
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        sut.getContent(file);
        verify(documentManager).getAsResource(file);
    }

    @Test
    void getContentThrowsUnsupportedAssetOperationWhenResourceIsNotFile() {
        final Resource resource = Generator.generateResourceWithId();
        assertThrows(UnsupportedAssetOperationException.class, () -> sut.getContent(resource));
        verify(documentManager, never()).getAsResource(any());
    }

    @Test
    void saveContentSavesFileContentViaDocumentManager() {
        final ByteArrayInputStream bis = new ByteArrayInputStream("test".getBytes());
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        sut.saveContent(file, bis);
        verify(documentManager).saveFileContent(file, bis);
    }

    @Test
    void saveContentThrowsUnsupportedAssetOperationExceptionWhenResourceIsNotFile() {
        final ByteArrayInputStream bis = new ByteArrayInputStream("test".getBytes());
        final Resource resource = Generator.generateResourceWithId();
        assertThrows(UnsupportedAssetOperationException.class, () -> sut.saveContent(resource, bis));
        verify(documentManager, never()).saveFileContent(any(), any());
    }

    @Test
    void saveContentCreatesBackupBeforeSavingFileContentInDocumentManager() {
        final ByteArrayInputStream bis = new ByteArrayInputStream("test".getBytes());
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        sut.saveContent(file, bis);
        final InOrder inOrder = Mockito.inOrder(documentManager);
        inOrder.verify(documentManager).createBackup(file);
        inOrder.verify(documentManager).saveFileContent(file, bis);
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisForSpecifiedFile() {
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        sut.runTextAnalysis(file);
        verify(textAnalysisService).analyzeFile(file);
    }

    @Test
    void runTextAnalysisThrowsUnsupportedAssetOperationWhenResourceIsNotFile() {
        final Resource resource = Generator.generateResourceWithId();
        assertThrows(UnsupportedAssetOperationException.class, () -> sut.runTextAnalysis(resource));
        verify(textAnalysisService, never()).analyzeFile(any());
    }
}