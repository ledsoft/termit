package cz.cvut.kbss.termit.model.selector;

import cz.cvut.kbss.jopa.CommonVocabulary;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.model.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_fragment_selector)
public class FragmentSelector extends TermSelector {

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = CommonVocabulary.RDF_VALUE)
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "FragmentSelector{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}
