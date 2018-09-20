package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@Service
public class TextAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(TextAnalysisService.class);

    private final RestTemplate restClient;

    private final Configuration config;

    private final AnnotationGenerator annotationGenerator;

    @Autowired
    public TextAnalysisService(RestTemplate restClient, Configuration config, AnnotationGenerator annotationGenerator) {
        this.restClient = restClient;
        this.config = config;
        this.annotationGenerator = annotationGenerator;
    }

    /**
     * Passes the content of the specified file to the remote text analysis service, letting it find occurrences of
     * terms from the vocabulary associated with the specified document in the text.
     * <p>
     * The analysis result is passed to the term occurrence generator.
     *
     * @param file     File whose content shall be analyzed
     * @param document Document to which the file belongs
     */
    @Async
    public void analyzeDocument(File file, Document document) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(document);
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(loadFileContent(file, document));
        input.setVocabularyContext(document.getVocabulary().getUri());
        input.setVocabularyRepository(URI.create(config.get(ConfigParam.REPOSITORY_URL)));
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
        try {
            final Resource result = restClient
                    .exchange(config.get(ConfigParam.TEXT_ANALYSIS_SERVICE_URL), HttpMethod.POST,
                            new HttpEntity<>(input, headers), Resource.class).getBody();
            annotationGenerator.generateAnnotations(result.getInputStream(), file, document.getVocabulary());
        } catch (RuntimeException e) {
            throw new WebServiceIntegrationException("Text analysis invocation failed.", e);
        } catch (IOException e) {
            throw new WebServiceIntegrationException("Unable to read text analysis result from response.", e);
        }
    }

    private String loadFileContent(File file, Document document) {
        try {
            final String path = config.get(ConfigParam.FILE_STORAGE) + java.io.File.separator +
                    document.getFileDirectoryName() + java.io.File.separator + file.getFileName();
            LOG.debug("Loading file for text analysis from {}.", path);
            final List<String> lines = Files.readAllLines(new java.io.File(path).toPath());
            return String.join("\n", lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
