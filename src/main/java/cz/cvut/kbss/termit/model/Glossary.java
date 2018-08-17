package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_glosar)
public class Glossary extends AbstractEntity {

    @OWLObjectProperty(iri = Vocabulary.s_p_obsahuje_pojem, fetch = FetchType.EAGER)
    private Set<Term> terms;

    public Set<Term> getTerms() {
        return terms;
    }

    public void setTerms(Set<Term> terms) {
        this.terms = terms;
    }

    @Override
    public String toString() {
        return "Glossary{" +
                "term count=" + (terms != null ? terms.size() : 0) +
                "} " + super.toString();
    }
}
