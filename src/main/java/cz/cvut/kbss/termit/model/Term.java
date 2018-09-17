package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_term)
public class Term implements Serializable {

    @Id
    private URI uri;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @OWLObjectProperty(iri = Vocabulary.s_p_narrower, cascade = {CascadeType.PERSIST,
            CascadeType.MERGE}, fetch = FetchType.EAGER)
    private Set<Term> subTerms; //TODO FetchType.LAZY ???

    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_vyskyt_termu, fetch = FetchType.EAGER)
    private Set<TermOccurrence> occurrences;

    @Types
    private Set<String> types;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Set<Term> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<Term> subTerms) {
        this.subTerms = subTerms;
    }

    public Set<TermOccurrence> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(Set<TermOccurrence> occurrences) {
        this.occurrences = occurrences;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term)) return false;
        Term term = (Term) o;
        return Objects.equals(uri, term.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "Term{" +
                label +
                " <" + uri + '>' +
                ", types=" + types +
                '}';
    }
}
