package cz.cvut.kbss.termit.model.selector;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Selector using text quote with prefix and suffix to identify the context.
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/#text-quote-selector">https://www.w3.org/TR/annotation-model/#text-quote-selector</a>
 */
@OWLClass(iri = Vocabulary.s_c_selektor_text_quote)
public class TextQuoteSelector extends TermSelector {

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_presny_text_quote)
    private String exactMatch;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_prefix_text_quote)
    private String prefix;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_suffix_text_quote)
    private String suffix;

    public TextQuoteSelector() {
    }

    public TextQuoteSelector(@NotBlank String exactMatch) {
        this.exactMatch = exactMatch;
    }

    public String getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(String exactMatch) {
        this.exactMatch = exactMatch;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextQuoteSelector)) {
            return false;
        }
        TextQuoteSelector selector = (TextQuoteSelector) o;
        return Objects.equals(exactMatch, selector.exactMatch) &&
                Objects.equals(prefix, selector.prefix) &&
                Objects.equals(suffix, selector.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exactMatch, prefix, suffix);
    }

    @Override
    public String toString() {
        return "TextQuoteSelector{" +
                "exactMatch='" + exactMatch + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                "} " + super.toString();
    }
}
