package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.dto.TextAnalysisInput;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
        final TextAnalysisInput input = createAnalysisInput(file, document);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
        try {
            final ResponseEntity<Resource> resp = restClient
                    .exchange(config.get(ConfigParam.TEXT_ANALYSIS_SERVICE_URL), HttpMethod.POST,
                            new HttpEntity<>(input, headers), Resource.class);
            if (!resp.hasBody()) {
                throw new WebServiceIntegrationException("Text analysis service returned empty response.");
            }
            assert resp.getBody() != null;
            final Resource resource = resp.getBody();
            // resource.getInputStream returns a fresh stream for each call, which is exactly what we need here
            try (final InputStream is = resource.getInputStream()) {
                writeOutAnalysisResult(is, file, document);
            }
            try (final InputStream is = resource.getInputStream()) {
                annotationGenerator.generateAnnotations(is, file, document.getVocabulary());
            }
        } catch (RuntimeException e) {
            if (e instanceof WebServiceIntegrationException) {
                throw e;
            }
            throw new WebServiceIntegrationException("Text analysis invocation failed.", e);
        } catch (IOException e) {
            throw new WebServiceIntegrationException("Unable to read text analysis result from response.", e);
        }
    }

    private TextAnalysisInput createAnalysisInput(File file, Document document) {
        final TextAnalysisInput input = new TextAnalysisInput();
        input.setContent(loadFileContent(file, document));
        input.setVocabularyContext(document.getVocabulary().getUri());
        input.setVocabularyRepository(URI.create(config.get(ConfigParam.REPOSITORY_URL)));
        return input;
    }

    private String loadFileContent(File file, Document document) {
        try {
            final String path = resolveFilePath(file, document);
            LOG.debug("Loading file for text analysis from {}.", path);
            final List<String> lines = Files.readAllLines(new java.io.File(path).toPath());
            return String.join("\n", lines);
        } catch (IOException e) {
            throw new TermItException("Unable to read file for text analysis.", e);
        }
    }

    private String resolveFilePath(File file, Document document) {
        return config.get(ConfigParam.FILE_STORAGE) + java.io.File.separator +
                document.getFileDirectoryName() + java.io.File.separator + file.getFileName();
    }

    private void writeOutAnalysisResult(InputStream input, File file, Document document) {
        try {
            final String path = resolveFilePath(file, document);
            Files.copy(input, new java.io.File(path).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TermItException("Unable to write out text analysis results.", e);
        }
    }
}
