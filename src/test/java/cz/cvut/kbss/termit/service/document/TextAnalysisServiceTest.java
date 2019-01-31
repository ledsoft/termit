package cz.cvut.kbss.termit.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;
import static cz.cvut.kbss.termit.util.ConfigParam.TEXT_ANALYSIS_SERVICE_URL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class TextAnalysisServiceTest extends BaseServiceTestRunner {

    private static final URI DOC_URI = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/text-analysis-test");
    private static final String FILE_NAME = "tas-test.html";

    private static final String CONTENT =
            "<html><body><h1>Metropolitan plan</h1><p>Description of the metropolitan plan.</body></html>";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Configuration config;

    @Autowired
    private DocumentManager documentManager;

    @Autowired
    private Environment environment;

    @Mock
    private AnnotationGenerator annotationGeneratorMock;

    private TextAnalysisService sut;

    private MockRestServiceServer mockServer;

    private ObjectMapper objectMapper;
    private DocumentManager documentManagerSpy;

    private DocumentVocabulary vocabulary;
    private Document document;

    private File file;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.objectMapper = cz.cvut.kbss.termit.environment.Environment.getObjectMapper();
        this.vocabulary = new DocumentVocabulary();
        vocabulary.setLabel("TestVocabulary");
        vocabulary.setUri(Generator.generateUri());
        this.document = new Document();
        document.setLabel("text-analysis-test");
        document.setUri(DOC_URI);
        document.setVocabulary(vocabulary.getUri());
        vocabulary.setDocument(document);
        this.file = new File();
        file.setLabel(FILE_NAME);
        file.setDocument(document);
        generateFile();
        this.documentManagerSpy = spy(documentManager);
        doCallRealMethod().when(documentManagerSpy).loadFileContent(any());
        doNothing().when(documentManagerSpy).createBackup(any());
        this.sut = new TextAnalysisService(restTemplate, config, documentManagerSpy, annotationGeneratorMock);
    }

    @Test
    void analyzeDocumentInvokesTextAnalysisServiceWithDocumentContent() {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST)).andExpect(content().string(containsString(CONTENT)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeFile(file);
        mockServer.verify();
    }

    private void generateFile() throws IOException {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final java.io.File docDir = new java.io.File(dir.getAbsolutePath() + java.io.File.separator +
                document.getFileDirectoryName());
        docDir.mkdir();
        docDir.deleteOnExit();
        final java.io.File content = new java.io.File(
                docDir.getAbsolutePath() + java.io.File.separator + FILE_NAME);
        Files.write(content.toPath(), CONTENT.getBytes());
        content.deleteOnExit();
    }

    @Test
    void analyzeDocumentPassesRepositoryAndVocabularyContextToService() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeFile(file);
        mockServer.verify();
    }

    private TextAnalysisInput textAnalysisInput() {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(REPOSITORY_URL)));
        return input;
    }

    @Test
    void analyzeDocumentPassesContentTypeAndAcceptHeadersToService() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                  .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeFile(file);
        mockServer.verify();
    }

    @Test
    void analyzeDocumentThrowsWebServiceIntegrationExceptionOnError() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withServerError());
        assertThrows(WebServiceIntegrationException.class, () -> sut.analyzeFile(file));
        mockServer.verify();
    }

    @Test
    void analyzeDocumentInvokesAnnotationGeneratorWithResultFromTextAnalysisService() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeFile(file);
        mockServer.verify();
        final ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(annotationGeneratorMock).generateAnnotations(captor.capture(), eq(file));
        final String result = new BufferedReader(new InputStreamReader(captor.getValue())).lines().collect(
                Collectors.joining("\n"));
        assertEquals(CONTENT, result);
    }

    @Test
    void analyzeDocumentThrowsNotFoundExceptionWhenFileCannotBeFound() {
        file.setLabel("unknown.html");
        final NotFoundException result = assertThrows(NotFoundException.class,
                () -> sut.analyzeFile(file));
        assertThat(result.getMessage(), containsString("not found on file system"));
    }

    @Test
    void analyzeDocumentThrowsWebServiceIntegrationExceptionWhenRemoteServiceReturnsEmptyBody() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess());
        final WebServiceIntegrationException result = assertThrows(WebServiceIntegrationException.class,
                () -> sut.analyzeFile(file));
        assertThat(result.getMessage(), containsString("empty response"));
        mockServer.verify();
    }

    @Test
    void analyzeDocumentCreatesFileBackupBeforeInvokingAnnotationGenerator() throws Exception {
        final TextAnalysisInput input = textAnalysisInput();
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeFile(file);
        mockServer.verify();
        final InOrder inOrder = Mockito.inOrder(documentManagerSpy, annotationGeneratorMock);
        inOrder.verify(documentManagerSpy).createBackup(file);
        inOrder.verify(annotationGeneratorMock).generateAnnotations(any(), eq(file));
    }
}