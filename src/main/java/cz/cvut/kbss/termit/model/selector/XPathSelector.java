package cz.cvut.kbss.termit.model.selector;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.jopa.vocabulary.RDF;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_selektor_xpath)
public class XPathSelector extends TermSelector {

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = RDF.VALUE)
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "XPathSelector{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}
