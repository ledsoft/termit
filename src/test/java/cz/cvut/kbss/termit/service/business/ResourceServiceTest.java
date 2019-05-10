package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.UnsupportedAssetOperationException;
import cz.cvut.kbss.termit.model.TextAnalysisRecord;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceServiceTest {

    @Mock
    private ResourceRepositoryService resourceRepositoryService;

    @Mock
    private VocabularyService vocabularyService;

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
        when(resourceRepositoryService.getRequiredReference(resource.getUri())).thenReturn(resource);
        sut.remove(resource.getUri());
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
        when(documentManager.exists(file)).thenReturn(true);
        sut.saveContent(file, bis);
        final InOrder inOrder = Mockito.inOrder(documentManager);
        inOrder.verify(documentManager).createBackup(file);
        inOrder.verify(documentManager).saveFileContent(file, bis);
    }

    @Test
    void saveContentDoesNotCreateBackupWhenFileDoesNotYetExist() {
        final ByteArrayInputStream bis = new ByteArrayInputStream("test".getBytes());
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        when(documentManager.exists(file)).thenReturn(false);
        sut.saveContent(file, bis);
        verify(documentManager, never()).createBackup(file);
        verify(documentManager).saveFileContent(file, bis);
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisForSpecifiedFile() {
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        sut.runTextAnalysis(file, Collections.emptySet());
        verify(textAnalysisService).analyzeFile(file);
    }

    @Test
    void runTextAnalysisThrowsUnsupportedAssetOperationWhenResourceIsNotFile() {
        final Resource resource = Generator.generateResourceWithId();
        assertThrows(UnsupportedAssetOperationException.class,
                () -> sut.runTextAnalysis(resource, Collections.emptySet()));
        verify(textAnalysisService, never()).analyzeFile(any());
    }

    @Test
    void runTextAnalysisInvokesAnalysisWithCustomVocabulariesWhenSpecified() {
        final File file = new File();
        file.setLabel("Test");
        file.setUri(Generator.generateUri());
        final Set<URI> vocabularies = new HashSet<>(Arrays.asList(Generator.generateUri(), Generator.generateUri()));
        sut.runTextAnalysis(file, vocabularies);
        verify(textAnalysisService).analyzeFile(file, vocabularies);
    }

    @Test
    void findAssignmentsDelegatesCallToRepositoryService() {
        final Resource resource = Generator.generateResourceWithId();
        sut.findAssignments(resource);
        verify(resourceRepositoryService).findAssignments(resource);
    }

    @Test
    void getReferenceDelegatesCallToRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.getReference(uri);
        verify(resourceRepositoryService).getReference(uri);
    }

    @Test
    void getRequiredReferenceDelegatesCallToRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.getRequiredReference(uri);
        verify(resourceRepositoryService).getRequiredReference(uri);
    }

    @Test
    void getFilesReturnsFilesFromDocument() {
        final Document doc = new Document();
        doc.setLabel("test document");
        doc.setUri(Generator.generateUri());
        final File fOne = Generator.generateFileWithId("test.html");
        doc.addFile(fOne);
        when(resourceRepositoryService.findRequired(doc.getUri())).thenReturn(doc);
        final List<File> result = sut.getFiles(doc);
        assertEquals(doc.getFiles().size(), result.size());
        assertTrue(doc.getFiles().containsAll(result));
        verify(resourceRepositoryService).findRequired(doc.getUri());
    }

    @Test
    void getFilesReturnsFilesSortedByLabel() {
        final Document doc = new Document();
        doc.setLabel("test document");
        doc.setUri(Generator.generateUri());
        final File fOne = Generator.generateFileWithId("test.html");
        doc.addFile(fOne);
        final File fTwo = Generator.generateFileWithId("act.html");
        doc.addFile(fTwo);
        when(resourceRepositoryService.findRequired(doc.getUri())).thenReturn(doc);
        final List<File> result = sut.getFiles(doc);
        assertEquals(Arrays.asList(fTwo, fOne), result);
    }

    @Test
    void getFilesReturnsEmptyListWhenDocumentHasNoFiles() {
        final Document doc = new Document();
        doc.setLabel("test document");
        doc.setUri(Generator.generateUri());
        when(resourceRepositoryService.findRequired(doc.getUri())).thenReturn(doc);
        final List<File> result = sut.getFiles(doc);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFilesThrowsUnsupportedAssetOperationExceptionWhenSpecifiedResourceIsNotDocument() {
        final Resource resource = Generator.generateResourceWithId();
        when(resourceRepositoryService.findRequired(resource.getUri())).thenReturn(resource);
        assertThrows(UnsupportedAssetOperationException.class, () -> sut.getFiles(resource));
    }

    @Test
    void addFileToDocumentPersistsFileAndUpdatesDocumentWithAddedFile() {
        final Document doc = Generator.generateDocumentWithId();
        final File fOne = Generator.generateFileWithId("test.html");
        sut.addFileToDocument(doc, fOne);
        verify(resourceRepositoryService).persist(fOne);
        verify(resourceRepositoryService).update(doc);
    }

    @Test
    void addFileToDocumentThrowsUnsupportedAssetOperationExceptionWhenSpecifiedResourceIsNotDocument() {
        final Resource resource = Generator.generateResourceWithId();
        final File fOne = Generator.generateFileWithId("test.html");
        assertThrows(UnsupportedAssetOperationException.class, () -> sut.addFileToDocument(resource, fOne));
    }

    @Test
    void addFileToDocumentPersistsFileIntoVocabularyContextForDocumentWithVocabulary() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Document doc = Generator.generateDocumentWithId();
        doc.setVocabulary(vocabulary.getUri());
        final File fOne = Generator.generateFileWithId("test.html");
        when(vocabularyService.getRequiredReference(vocabulary.getUri())).thenReturn(vocabulary);

        sut.addFileToDocument(doc, fOne);
        verify(resourceRepositoryService).persist(fOne, vocabulary);
        verify(vocabularyService).getRequiredReference(vocabulary.getUri());
    }

    @Test
    void addFileToDocumentUpdatesDocumentInVocabularyContextForDocumentWithVocabulary() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Document doc = Generator.generateDocumentWithId();
        doc.setVocabulary(vocabulary.getUri());
        final File fOne = Generator.generateFileWithId("test.html");
        when(vocabularyService.getRequiredReference(vocabulary.getUri())).thenReturn(vocabulary);

        sut.addFileToDocument(doc, fOne);
        verify(resourceRepositoryService).update(doc, vocabulary);
        verify(vocabularyService).getRequiredReference(vocabulary.getUri());
    }

    @Test
    void findLatestTextAnalysisRecordRetrievesLatestTextAnalysisRecordForResource() {
        final File file = Generator.generateFileWithId("test.html");
        final TextAnalysisRecord record = new TextAnalysisRecord(new Date(), file);
        when(textAnalysisService.findLatestAnalysisRecord(file)).thenReturn(Optional.of(record));

        final TextAnalysisRecord result = sut.findLatestTextAnalysisRecord(file);
        assertEquals(record, result);
        verify(textAnalysisService).findLatestAnalysisRecord(file);
    }

    @Test
    void findLatestTextAnalysisRecordThrowsNotFoundExceptionWhenNoRecordExists() {
        final Resource resource = Generator.generateResourceWithId();
        when(textAnalysisService.findLatestAnalysisRecord(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sut.findLatestTextAnalysisRecord(resource));
        verify(textAnalysisService).findLatestAnalysisRecord(resource);
    }

    @Test
    void hasContentChecksForContentExistenceInDocumentManager() {
        final File file = Generator.generateFileWithId("test.html");
        sut.hasContent(file);
        verify(documentManager).exists(file);
    }

    @Test
    void hasContentReturnsFalseForNonFile() {
        final Resource resource = Generator.generateResourceWithId();
        assertFalse(sut.hasContent(resource));
        verify(documentManager, never()).exists(any(File.class));
    }
}