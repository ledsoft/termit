package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    }
}
