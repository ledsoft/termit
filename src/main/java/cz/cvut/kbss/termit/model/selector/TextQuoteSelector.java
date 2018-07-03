package cz.cvut.kbss.termit.model.selector;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_text_quote_selector)
public class TextQuoteSelector extends TermSelector {

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_text_quote_exact)
    private String exactMatch;

    @OWLDataProperty(iri = Vocabulary.s_p_text_quote_prefix)
    private String prefix;

    @OWLDataProperty(iri = Vocabulary.s_p_text_quote_suffix)
    private String suffix;

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
    public String toString() {
        return "TextQuoteSelector{" +
                "exactMatch='" + exactMatch + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                "} " + super.toString();
    }
}
