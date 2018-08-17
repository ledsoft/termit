package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_cil)
public class Target extends AbstractEntity {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_selektor, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<TermSelector> selectors;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_zdrojovy_dokument, fetch = FetchType.EAGER)
    private Document source;

    public Set<TermSelector> getSelectors() {
        return selectors;
    }

    public void setSelectors(Set<TermSelector> selectors) {
        this.selectors = selectors;
    }

    public Document getSource() {
        return source;
    }

    public void setSource(Document source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "Target{" +
                "selectors=" + selectors +
                ", source=" + source +
                "} " + super.toString();
    }
}
