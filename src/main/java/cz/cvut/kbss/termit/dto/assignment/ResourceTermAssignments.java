package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

/**
 * Represents information about Term assignment to a Resource.
 * <p>
 * This provides only basic information about the Term - its identifier, label and identifier of a vocabulary to which
 * it belongs.
 */
@SparqlResultSetMapping(name = "ResourceTermAssignments", classes = @ConstructorResult(
        targetClass = ResourceTermAssignments.class,
        variables = {
                @VariableResult(name = "term", type = URI.class),
                @VariableResult(name = "label", type = String.class),
                @VariableResult(name = "vocabulary", type = URI.class),
                @VariableResult(name = "res", type = URI.class),
                @VariableResult(name = "suggested", type = Boolean.class)
        }
))
public class ResourceTermAssignments implements HasTypes {

    @OWLObjectProperty(iri = Vocabulary.s_p_je_prirazenim_termu)
    private URI term;

    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String termLabel;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_zdroj)
    private URI resource;

    @Types
    private Set<String> types;

    public ResourceTermAssignments() {
    }

    public ResourceTermAssignments(URI term, String termLabel, URI vocabulary, URI resource, Boolean suggested) {
        this.term = term;
        this.termLabel = termLabel;
        this.vocabulary = vocabulary;
        this.resource = resource;
        addType(Vocabulary.s_c_prirazeni_termu);
        if (suggested) {
            addType(Vocabulary.s_c_navrzene_prirazeni_termu);
        }
    }

    public URI getTerm() {
        return term;
    }

    public void setTerm(URI term) {
        this.term = term;
    }

    public String getTermLabel() {
        return termLabel;
    }

    public void setTermLabel(String termLabel) {
        this.termLabel = termLabel;
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

    public URI getResource() {
        return resource;
    }

    public void setResource(URI resource) {
        this.resource = resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceTermAssignments)) {
            return false;
        }
        ResourceTermAssignments that = (ResourceTermAssignments) o;
        return Objects.equals(term, that.term) &&
                Objects.equals(termLabel, that.termLabel) &&
                Objects.equals(vocabulary, that.vocabulary) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, termLabel, vocabulary, resource, types);
    }

    @Override
    public String toString() {
        return "ResourceTermAssignments{" +
                "term=" + term +
                ", termLabel='" + termLabel + '\'' +
                ", vocabulary=" + vocabulary +
                ", resource=" + resource +
                ", types=" + types +
                '}';
    }
}
