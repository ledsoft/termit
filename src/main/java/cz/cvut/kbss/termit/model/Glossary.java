package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.CascadeType;
import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_glosar)
public class Glossary extends AbstractEntity {

    /**
     * This attribute should contain only root terms. The term hierarchy is modelled by terms having sub-terms, so all
     * terms should be reachable.
     */
    @OWLObjectProperty(iri = Vocabulary.s_p_obsahuje_pojem, cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
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
    public boolean addTerm(Term term) {
        Objects.requireNonNull(term);
        if (terms == null) {
            this.terms = new HashSet<>();
        }
        return terms.add(term);
    }

    @Override
    public String toString() {
        return "Glossary{" +
                "term count=" + (terms != null ? terms.size() : 0) +
                " " + super.toString() + "}";
    }

    public static Field getTermsField() {
        try {
            return Glossary.class.getDeclaredField("terms");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"terms\" field.", e);
        }
    }
}
