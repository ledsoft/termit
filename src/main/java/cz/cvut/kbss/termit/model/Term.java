package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_term)
public class Term implements Serializable, HasTypes {

    @Id
    private URI uri;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @OWLDataProperty(iri = "http://purl.org/dc/elements/1.1/source")
    private Set<String> sources;

    @OWLObjectProperty(iri = Vocabulary.s_p_narrower, fetch = FetchType.EAGER)
    private Set<URI> subTerms;

    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @Properties(fetchType = FetchType.EAGER)
    private Map<String, Set<String>> properties;

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

    public Set<URI> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<URI> subTerms) {
        this.subTerms = subTerms;
    }

    public void addSubTerm(URI childUri) {
        Objects.requireNonNull(childUri);
        if (subTerms == null) {
            this.subTerms = new HashSet<>();
        }
        subTerms.add(childUri);
    }

    public Set<String> getSources() {
        return sources;
    }

    public void setSources(Set<String> source) {
        this.sources = source;
    }

    public URI getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(URI vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public Set<String> getTypes() {
        return types;
    }

    @Override
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Set<String>> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Term)) {
            return false;
        }
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
