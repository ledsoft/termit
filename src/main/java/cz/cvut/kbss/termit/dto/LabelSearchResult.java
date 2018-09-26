package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

@SparqlResultSetMapping(name = "LabelSearchResult", classes = {@ConstructorResult(targetClass = LabelSearchResult.class,
        variables = {
                @VariableResult(name = "x", type = URI.class),
                @VariableResult(name = "label"),
                @VariableResult(name = "vocabularyUri", type = URI.class),
                @VariableResult(name = "type", type = String.class)
        })})
public class LabelSearchResult implements Serializable {

    @Id
    private URI uri;

    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabularyUri;

    @Types
    private Set<String> types;

    public LabelSearchResult() {
    }

    public LabelSearchResult(URI uri, String label, URI vocabularyUri, String type) {
        this.uri = uri;
        this.label = label;
        this.vocabularyUri = vocabularyUri;
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

    public URI getVocabularyUri() {
        return vocabularyUri;
    }

    public void setVocabularyUri(URI vocabularyUri) {
        this.vocabularyUri = vocabularyUri;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    @Override
    public String toString() {
        return "LabelSearchResult{" +
                "uri=" + uri +
                ", label='" + label + '\'' +
                ", types=" + types +
                '}';
    }
}
