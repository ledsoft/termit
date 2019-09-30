package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_cil_vyskytu)
public class OccurrenceTarget extends Target {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_selektor_termu, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<TermSelector> selectors;

    public OccurrenceTarget() {
    }

    public OccurrenceTarget(File source) {
        super(source);
    }

    public Set<TermSelector> getSelectors() {
        return selectors;
    }

    public void setSelectors(Set<TermSelector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public String toString() {
        return "OccurrenceTarget{" +
                "selectors=" + selectors +
                "} " + super.toString();
    }
}
