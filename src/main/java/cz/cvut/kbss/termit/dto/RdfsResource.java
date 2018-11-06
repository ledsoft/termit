package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Representation of any RDFS resource.
 */
@SparqlResultSetMapping(name = "RdfsResource", classes = {@ConstructorResult(targetClass = RdfsResource.class,
        variables = {
                @VariableResult(name = "x", type = URI.class),
                @VariableResult(name = "label", type = String.class),
                @VariableResult(name = "comment", type = String.class),
                @VariableResult(name = "type", type = String.class)
        })})
public class RdfsResource implements Serializable {

    @Id
    private URI uri;

    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @Types
    private Set<String> types;

    public RdfsResource() {
    }

    public RdfsResource(URI uri, String label, String comment, String type) {
        this.uri = uri;
        this.label = label;
        this.comment = comment;
        this.types = Collections.singleton(type);
    }

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

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RdfsResource)) {
            return false;
        }
        RdfsResource that = (RdfsResource) o;
        return Objects.equals(uri, that.uri) &&
                Objects.equals(label, that.label) &&
                Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, label, types);
    }

    @Override
    public String toString() {
        return "RdfsResource{" +
                "uri=" + uri +
                ", label='" + label + '\'' +
                '}';
    }
}
