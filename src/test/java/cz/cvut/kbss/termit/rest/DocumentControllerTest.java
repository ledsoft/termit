package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.DocumentManager;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest extends BaseControllerTestRunner {

    private static final String[] FILE_NAMES = {"mpp.html", "mpp-auxiliary.html", "mpp-definitions.html"};
    private static final String NORMALIZED_DOC_NAME = "metropolitan-plan";

    private static final String PATH = "/documents";

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private DocumentRepositoryService documentServiceMock;

    @Mock
    private DocumentManager documentManagerMock;

    @Mock
    private TextAnalysisService textAnalysisServiceMock;

    @InjectMocks
    private DocumentController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisOnSpecifiedFileInDocument() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_DOC_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String fileName = FILE_NAMES[Generator.randomIndex(FILE_NAMES)];
        mockMvc.perform(
                put(PATH + "/" + NORMALIZED_DOC_NAME + "/text-analysis")
                        .param("namespace", Vocabulary.ONTOLOGY_IRI_termit)
                        .param("file", fileName))
               .andExpect(status().isAccepted());
        verify(textAnalysisServiceMock).analyzeDocument(getFile(doc, fileName), doc);
    }

    private static Document generateTestData() {
        final Document doc = new Document();
        doc.setUri(Generator.generateUri());
        doc.setName("Metropolitan Plan");
        final DocumentVocabulary vocabulary = new DocumentVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setName("Test Vocabulary");
        vocabulary.setDocument(doc);
        doc.setVocabulary(vocabulary);
        for (String name : FILE_NAMES) {
            final File file = new File();
            file.setFileName(name);
            doc.addFile(file);
        }
        return doc;
    }

    private static File getFile(Document doc, String name) {
        return doc.getFiles().stream().filter(f -> f.getFileName().equals(name)).findFirst()
                  .orElseThrow(() -> new TermItException("File not found."));
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisForAllFilesInDocumentWhenFileNameIsNotSpecified() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_DOC_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        mockMvc.perform(
                put(PATH + "/" + NORMALIZED_DOC_NAME + "/text-analysis")
                        .param("namespace", Vocabulary.ONTOLOGY_IRI_termit))
               .andExpect(status().isAccepted());
        for (File f : doc.getFiles()) {
            verify(textAnalysisServiceMock).analyzeDocument(f, doc);
        }
    }

    @Test
    void runTextAnalysisThrowsNotFoundExceptionForUnknownDocumentId() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_DOC_NAME))).thenReturn(doc.getUri());
        final MvcResult mvcResult = mockMvc.perform(
                put(PATH + "/" + NORMALIZED_DOC_NAME + "/text-analysis")
                        .param("namespace", Vocabulary.ONTOLOGY_IRI_termit))
                                           .andExpect(status().isNotFound()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString(doc.getUri().toString()));
        verify(textAnalysisServiceMock, never()).analyzeDocument(any(), any());
    }

    @Test
    void runTextAnalysisThrowsNotFoundExceptionForUnknownFileName() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_DOC_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String fileName = "unknownFile";
        final MvcResult mvcResult = mockMvc.perform(
                put(PATH + "/" + NORMALIZED_DOC_NAME + "/text-analysis")
                        .param("namespace", Vocabulary.ONTOLOGY_IRI_termit)
                        .param("file", fileName))
                                           .andExpect(status().isNotFound()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString(fileName));
        verify(textAnalysisServiceMock, never()).analyzeDocument(any(), any());
    }

    @Test
    void getDocumentReturnsDocumentWithIdExtractedFromDocumentNameAndDefaultNamespace() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(any(ConfigParam.class), eq(NORMALIZED_DOC_NAME)))
                .thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + NORMALIZED_DOC_NAME))
                                           .andExpect(status().isOk()).andReturn();
        final Document result = readValue(mvcResult, Document.class);
        assertNotNull(result);
        assertEquals(doc.getUri(), result.getUri());
        assertEquals(doc.getName(), result.getName());
        assertEquals(doc.getFiles().size(), result.getFiles().size());
        verify(idResolverMock).resolveIdentifier(ConfigParam.NAMESPACE_DOCUMENT, NORMALIZED_DOC_NAME);
    }

    @Test
    void getDocumentReturnsDocumentWithIdExtractedFromDocumentNameAndSpecifiedNamespace() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_DOC_NAME)))
                .thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String namespace = Vocabulary.ONTOLOGY_IRI_termit;
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + NORMALIZED_DOC_NAME).param("namespace", namespace))
                                           .andExpect(status().isOk()).andReturn();
        final Document result = readValue(mvcResult, Document.class);
        assertNotNull(result);
        assertEquals(doc.getUri(), result.getUri());
        assertEquals(doc.getName(), result.getName());
        assertEquals(doc.getFiles().size(), result.getFiles().size());
        verify(idResolverMock).resolveIdentifier(namespace, NORMALIZED_DOC_NAME);
    }

    @Test
    void getFileContentReturnsContentOfRequestedFile() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(any(ConfigParam.class), eq(NORMALIZED_DOC_NAME)))
                .thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final java.io.File content = Files.createTempFile("document", ".html").toFile();
        content.deleteOnExit();
        final String data = "<html><head><title>Test</title></head><body>test</body></html>";
        Files.write(content.toPath(), data.getBytes());
        when(documentManagerMock.getAsResource(doc, getFile(doc, FILE_NAMES[0])))
                .thenReturn(new FileSystemResource(content));
        when(documentManagerMock.getMediaType(doc, getFile(doc, FILE_NAMES[0])))
                .thenReturn(Optional.of(MediaType.TEXT_HTML_VALUE));
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + NORMALIZED_DOC_NAME + "/content").param("file", FILE_NAMES[0]))
                .andExpect(status().isOk()).andReturn();
        final String resultContent = mvcResult.getResponse().getContentAsString();
        assertEquals(data, resultContent);
        assertEquals(MediaType.TEXT_HTML_VALUE, mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void getFileContentThrowsNotFoundExceptionForUnknownFileName() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(any(ConfigParam.class), eq(NORMALIZED_DOC_NAME)))
                .thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String name = "unknown.txt";
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + NORMALIZED_DOC_NAME + "/content").param("file", name))
                .andExpect(status().isNotFound()).andReturn();
        final ErrorInfo result = readValue(mvcResult, ErrorInfo.class);
        assertNotNull(result);
        assertThat(result.getMessage(), containsString("not found in document"));
        verify(documentManagerMock, never()).getAsResource(any(), any());
    }

    @Test
    void getAllReturnsDocumentsFromService() throws Exception {
        final List<Document> documents = IntStream.range(0, 10).mapToObj(i -> {
            final Document doc = new Document();
            doc.setName("Document-" + i);
            doc.setUri(Generator.generateUri());
            return doc;
        }).collect(Collectors.toList());
        when(documentServiceMock.findAll()).thenReturn(documents);
        final MvcResult mvcResult = mockMvc.perform(get(PATH)).andExpect(status().isOk()).andReturn();
        final List<Document> result = readValue(mvcResult, new TypeReference<List<Document>>() {
        });
        assertNotNull(result);
        assertEquals(documents, result);
        verify(documentServiceMock).findAll();
    }
}