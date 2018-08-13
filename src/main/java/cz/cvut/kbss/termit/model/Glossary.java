package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_glossary)
public class Glossary extends AbstractEntity {

    @OWLObjectProperty(iri = Vocabulary.s_p_has_term, fetch = FetchType.EAGER)
    private Set<Term> terms;

    public Set<Term> getTerms() {
        return terms;
    }

    public void setTerms(Set<Term> terms) {
        this.terms = terms;
    }

    /**
     * Adds the specified term into this glossary.
     *
     * @param term Term to add
     */
    public void addTerm(Term term) {
        Objects.requireNonNull(term);
        if (terms == null) {
            this.terms = new HashSet<>();
        }
        terms.add(term);
    }

    @Override
    public String toString() {
        return "Glossary{" +
                "term count=" + (terms != null ? terms.size() : 0) +
                "} " + super.toString();
    }
}
