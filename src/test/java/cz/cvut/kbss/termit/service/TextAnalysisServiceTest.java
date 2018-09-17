package cz.cvut.kbss.termit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;
import static cz.cvut.kbss.termit.util.ConfigParam.TEXT_ANALYSIS_SERVICE_URL;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TextAnalysisServiceTest extends BaseServiceTestRunner {

    private static final String CONTENT = "<html><body><h1>Metropolitan plan</h1><p>Description of the metropolitan plan.</body></html>";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private TextAnalysisService sut;

    private MockRestServiceServer mockServer;

    private ObjectMapper objectMapper;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.objectMapper = cz.cvut.kbss.termit.environment.Environment.getObjectMapper();
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
    }

    @Test
    void analyzeDocumentInvokesTextAnalysisServiceWithDocumentContent() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        mockServer.expect(requestTo(environment.getRequiredProperty(TEXT_ANALYSIS_SERVICE_URL.toString())))
                  .andExpect(method(HttpMethod.POST)).andExpect(content().string(containsString(CONTENT)))
                  .andRespond(withSuccess());
        final File file = new File();
        file.setName("Test");
        file.setLocation(generateFile());
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
        input.setVocabularyRepository(URI.create(environment.getRequiredProperty(REPOSITORY_URL.toString())));
        mockServer.expect(requestTo(environment.getRequiredProperty(TEXT_ANALYSIS_SERVICE_URL.toString())))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andRespond(withSuccess());
        final File file = new File();
        file.setName("Test");
        file.setLocation(generateFile());
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
    }

    @Test
    void analyzeDocumentPassesContentTypeAndAcceptHeadersToService() throws Exception {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(CONTENT);
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(environment.getRequiredProperty(REPOSITORY_URL.toString())));
        mockServer.expect(requestTo(environment.getRequiredProperty(TEXT_ANALYSIS_SERVICE_URL.toString())))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().string(objectMapper.writeValueAsString(input)))
                  .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
                  .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE))
                  .andRespond(withSuccess());
        final File file = new File();
        file.setName("Test");
        file.setLocation(generateFile());
        sut.analyzeDocument(file, vocabulary);
        mockServer.verify();
    }
}