package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

@SparqlResultSetMapping(name = "FullTextSearchResult", classes = {@ConstructorResult(targetClass = FullTextSearchResult.class,
        variables = {
                @VariableResult(name = "entity", type = URI.class),
                @VariableResult(name = "label"),
                @VariableResult(name = "vocabularyUri", type = URI.class),
                @VariableResult(name = "type", type = String.class),
                @VariableResult(name = "snippetField", type = String.class),
                @VariableResult(name = "snippetText", type = String.class),
                @VariableResult(name = "score", type = Double.class)
        })})
public class FullTextSearchResult implements Serializable {

    @Id
    private URI uri;

    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLDataProperty(iri =  Vocabulary.ONTOLOGY_IRI_slovnik + "/fts/snippet-text")
    private String snippetText;

    @OWLDataProperty(iri = Vocabulary.ONTOLOGY_IRI_slovnik + "/fts/snippet-field")
    private String snippetField;

    @OWLDataProperty(iri = Vocabulary.ONTOLOGY_IRI_slovnik + "/fts/score")
    private Double score;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @Types
    private Set<String> types;

    public FullTextSearchResult() {
    }

    public FullTextSearchResult(URI uri, String label, URI vocabulary, String type, String snippetField, String snippetText, Double score) {
        this.uri = uri;
        this.label = label;
        this.vocabulary = vocabulary;
        this.types = Collections.singleton(type);
        this.snippetField = snippetField;
        this.snippetText = snippetText;
        this.score = score;
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

    public URI getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(URI vocabulary) {
        this.vocabulary = vocabulary;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public String getSnippetText() {
        return snippetText;
    }

    public void setSnippetText(String snippetText) {
        this.snippetText = snippetText;
    }

    public String getSnippetField() {
        return snippetField;
    }

    public void setSnippetField(String snippetField) {
        this.snippetField = snippetField;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "FullTextSearchResult{" +
                "uri=" + uri +
                ", label='" + label + '\'' +
                ", vocabulary=" + vocabulary +
                ", types=" + types +
                ", snippetText='" + snippetText + '\'' +
                ", snippetField='" + snippetField + '\'' +
                ", score=" + score +
                '}';
    }
}
