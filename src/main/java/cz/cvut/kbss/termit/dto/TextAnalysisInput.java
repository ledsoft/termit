package cz.cvut.kbss.termit.dto;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents input passed to the text analysis service.
 * <p>
 * Mainly contains the content to analyze and identification of the vocabularies whose terms will be used in the text
 * analysis.
 */
public class TextAnalysisInput {

    /**
     * Text content to analyze.
     */
    private String content;

    /**
     * URI of the repository containing vocabularies whose terms are used in the text analysis.
     */
    private URI vocabularyRepository;

    /**
     * URIs of contexts containing vocabularies whose terms are used in the text analysis. Optional.
     * <p>
     * If not specified, the whole {@link #vocabularyRepository} is searched for terms.
     */
    private Set<URI> vocabularyContexts;

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

    public Set<URI> getVocabularyContexts() {
        return vocabularyContexts;
    }

    public void setVocabularyContexts(Set<URI> vocabularyContexts) {
        this.vocabularyContexts = vocabularyContexts;
    }

    public void addVocabularyContext(URI vocabularyContext) {
        if (vocabularyContexts == null) {
            this.vocabularyContexts = new HashSet<>();
        }
        vocabularyContexts.add(vocabularyContext);
    }

    @Override
    public String toString() {
        return "TextAnalysisInput{" +
                "content='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", vocabularyRepository=" + vocabularyRepository +
                ", vocabularyContexts=" + vocabularyContexts +
                '}';
    }
}
