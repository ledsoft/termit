package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@Service
public class TextAnalysisService {

    private final RestTemplate restClient;

    private final Configuration config;

    @Autowired
    public TextAnalysisService(RestTemplate restClient, Configuration config) {
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * Passes the content of the specified file to the remote text analysis service, letting it find occurrences of
     * terms from the specified vocabulary in the text.
     * <p>
     * The analysis result is passed to the term occurrence generator.
     *
     * @param file       File whose content shall be analyzed
     * @param vocabulary Vocabulary used for text analysis
     */
    public void analyzeDocument(File file, Vocabulary vocabulary) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(vocabulary);
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(loadFileContent(file));
        input.setVocabularyContext(vocabulary.getUri());
        input.setVocabularyRepository(URI.create(config.get(ConfigParam.REPOSITORY_URL)));
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
        final String result = restClient.exchange(config.get(ConfigParam.TEXT_ANALYSIS_SERVICE_URL), HttpMethod.POST,
                new HttpEntity<>(input, headers), String.class).getBody();
    }

    private String loadFileContent(File file) {
        try {
            final List<String> lines = Files.readAllLines(new java.io.File(file.getLocation()).toPath());
            return String.join("\n", lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
