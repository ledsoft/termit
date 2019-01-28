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
    @OWLObjectProperty(iri = Vocabulary.s_p_obsahuje_korenovy_pojem, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Set<Term> rootTerms;

    public Set<Term> getRootTerms() {
        return rootTerms;
    }

    public void setRootTerms(Set<Term> rootTerms) {
        this.rootTerms = rootTerms;
    }

    /**
     * Adds the specified root term into this glossary.
     *
     * @param rootTerm Term to add
     */
    public boolean addRootTerm(Term rootTerm) {
        Objects.requireNonNull(rootTerm);
        if (rootTerms == null) {
            this.rootTerms = new HashSet<>();
        }
        return rootTerms.add(rootTerm);
    }

    @Override
    public String toString() {
        return "Glossary{" +
               "term count=" + (rootTerms != null ? rootTerms.size() : 0) +
               " " + super.toString() + "}";
    }

    public static Field getTermsField() {
        try {
            return Glossary.class.getDeclaredField("rootTerms");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"terms\" field.", e);
        }
    }
}
