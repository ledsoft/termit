package cz.cvut.kbss.termit.dto;

import java.net.URI;

/**
 * Represents input passed to the text analysis service.
 * <p>
 * Mainly contains the content to analyze and identification of the vocabulary whose terms will be used in the text
 * analysis.
 */
public class TextAnalysisInput {

    /**
     * Text content to analyze.
     */
    private String content;

    /**
     * URI of the repository containing vocabulary whose terms are used in the text analysis.
     */
    private URI vocabularyRepository;

    /**
     * URI of the context containing vocabulary whose terms are used in the text analysis. Optional.
     */
    private URI vocabularyContext;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public URI getVocabularyRepository() {
        return vocabularyRepository;
    }

    public void setVocabularyRepository(URI vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public URI getVocabularyContext() {
        return vocabularyContext;
    }

    public void setVocabularyContext(URI vocabularyContext) {
        this.vocabularyContext = vocabularyContext;
    }
}
