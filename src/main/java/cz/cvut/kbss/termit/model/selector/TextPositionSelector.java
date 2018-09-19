package cz.cvut.kbss.termit.model.selector;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.util.Vocabulary;

/**
 * Position-based selector of text in a document.
 * <p>
 * Note that position selector is susceptible to even minor changes in the document content (added or removed
 * whitespaces etc.).
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/#text-position-selector">https://www.w3.org/TR/annotation-model/#text-position-selector</a>
 */
@OWLClass(iri = Vocabulary.s_c_selektor_pozici_v_textu)
public class TextPositionSelector extends TermSelector {

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_startovni_pozici)
    private Integer start;

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_koncovou_pozici)
    private Integer end;

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "TextPositionSelector{" +
                "start=" + start +
                ", end=" + end +
                "} " + super.toString();
    }
}
