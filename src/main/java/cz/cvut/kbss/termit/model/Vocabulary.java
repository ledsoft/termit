package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.asset.provenance.ProvenanceManager;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@OWLClass(iri = cz.cvut.kbss.termit.util.Vocabulary.s_c_slovnik)
@JsonLdAttributeOrder({"uri", "label", "comment", "author", "lastEditor"})
@EntityListeners(ProvenanceManager.class)
public class Vocabulary extends Asset implements Serializable {

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_glosar, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Glossary glossary;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_model, cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER)
    private Model model;

    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_importuje_slovnik, fetch = FetchType.EAGER)
    private Set<URI> importedVocabularies;

    @Properties(fetchType = FetchType.EAGER)
    private Map<String, Set<String>> properties;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Glossary getGlossary() {
        return glossary;
    }

    public void setGlossary(Glossary glossary) {
        this.glossary = glossary;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Set<URI> getImportedVocabularies() {
        return importedVocabularies;
    }

    public void setImportedVocabularies(Set<URI> importedVocabularies) {
        this.importedVocabularies = importedVocabularies;
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
        if (!(o instanceof Vocabulary)) {
            return false;
        }
        Vocabulary that = (Vocabulary) o;
        return Objects.equals(getUri(), that.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Vocabulary{" +
                getLabel() +
                " <" + getUri() + '>' +
                ", glossary=" + glossary +
                (importedVocabularies != null ?
                 ", importedVocabularies = [" + importedVocabularies.stream().map(p -> "<" + p + ">").collect(
                         Collectors.joining(", ")) + "]" : "") +
                '}';
    }

    public static Field getGlossaryField() {
        try {
            return Vocabulary.class.getDeclaredField("glossary");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"glossary\" field.", e);
        }
    }
}
