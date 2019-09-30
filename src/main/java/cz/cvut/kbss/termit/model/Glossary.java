package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_glosar)
public class Glossary extends AbstractEntity {

    /**
     * This attribute should contain only root terms. The term hierarchy is modelled by terms having sub-terms, so all
     * terms should be reachable.
     */
    @OWLObjectProperty(iri = Vocabulary.s_p_obsahuje_korenovy_pojem)
    private Set<URI> rootTerms;

    public Set<URI> getRootTerms() {
        return rootTerms;
    }

    public void setRootTerms(Set<URI> rootTerms) {
        this.rootTerms = rootTerms;
    }

    /**
     * Adds the specified root term into this glossary.
     *
     * @param rootTerm Term to add
     */
    public void addRootTerm(Term rootTerm) {
        Objects.requireNonNull(rootTerm);
        if (rootTerms == null) {
            this.rootTerms = new HashSet<>();
        }
        rootTerms.add(rootTerm.getUri());
    }

    /**
     * Removes the specified term from root terms of this glossary, if it were present.
     *
     * @param toRemove The term to remove from root terms
     */
    public void removeRootTerm(Term toRemove) {
        Objects.requireNonNull(toRemove);
        if (rootTerms != null) {
            rootTerms.remove(toRemove.getUri());
        }
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
            throw new TermItException("Fatal error! Unable to retrieve \"rootTerms\" field.", e);
        }
    }
}
