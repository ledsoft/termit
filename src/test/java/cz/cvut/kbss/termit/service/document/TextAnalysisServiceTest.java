package cz.cvut.kbss.termit.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TextAnalysisServiceTest extends BaseServiceTestRunner {

    private static final String CONTENT =
            "<html><body><h1>Metropolitan plan</h1><p>Description of the metropolitan plan.</body></html>";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Configuration config;

    @Mock
    private AnnotationGenerator annotationGeneratorMock;

    private TextAnalysisService sut;

    private MockRestServiceServer mockServer;

    private ObjectMapper objectMapper;

    private Vocabulary vocabulary;

    private File file;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.objectMapper = cz.cvut.kbss.termit.environment.Environment.getObjectMapper();
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        this.sut = new TextAnalysisService(restTemplate, config, annotationGeneratorMock);
        this.file = new File();
        file.setName("test");
        file.setLocation(generateFile());
    }

    @Test
    void analyzeDocumentInvokesTextAnalysisServiceWithDocumentContent() {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST)).andExpect(content().string(containsString(CONTENT)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
    }

    private static String generateFile() throws IOException {
        final java.io.File file = Files.createTempFile("tasTest", ".html").toFile();
        file.deleteOnExit();
        Files.write(file.toPath(), CONTENT.getBytes());
        return file.getAbsolutePath();
    }

    @Test
    void analyzeDocumentPassesRepositoryAndVocabularyContextToService() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(REPOSITORY_URL)));
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
    }

    @Test
    void analyzeDocumentPassesContentTypeAndAcceptHeadersToService() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(REPOSITORY_URL)));
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                  .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
    }

    @Test
    void analyzeDocumentThrowsWebServiceIntegrationExceptionOnError() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(REPOSITORY_URL)));
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withServerError());
        assertThrows(WebServiceIntegrationException.class, () -> sut.analyzeDocument(file, vocabulary));
        mockServer.verify();
    }

    @Test
    void analyzeDocumentInvokesAnnotationGeneratorWithResultFromTextAnalysisService() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(REPOSITORY_URL)));
        mockServer.expect(requestTo(config.get(TEXT_ANALYSIS_SERVICE_URL)))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess(CONTENT, MediaType.APPLICATION_XML));
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
        final ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(annotationGeneratorMock).generateAnnotations(captor.capture(), eq(file), eq(vocabulary));
        final String result = new BufferedReader(new InputStreamReader(captor.getValue())).lines().collect(
                Collectors.joining("\n"));
        assertEquals(CONTENT, result);
    }
}