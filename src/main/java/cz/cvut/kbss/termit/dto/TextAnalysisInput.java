/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
     * <p>
     * If not specified, the whole {@link #vocabularyRepository} is searched for terms.
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

    @Override
    public String toString() {
        return "TextAnalysisInput{" +
                "content='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", vocabularyRepository=" + vocabularyRepository +
                ", vocabularyContext=" + vocabularyContext +
                '}';
    }
}
