package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_dokument)
public class Document extends HasProvenanceData implements Serializable {

    @Id
    private URI uri;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String name;

    @OWLDataProperty(iri = Vocabulary.s_p_description)
    private String description;

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_soubor, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Set<File> files;

    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_dokumentovy_slovnik, fetch = FetchType.EAGER)
    private DocumentVocabulary vocabulary;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<File> getFiles() {
        return files;
    }

    public void setFiles(Set<File> files) {
        this.files = files;
    }

    public void addFile(File file) {
        Objects.requireNonNull(file);
        if (files == null) {
            this.files = new HashSet<>();
        }
        files.add(file);
    }

    public DocumentVocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(DocumentVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public String toString() {
        return "Document{" +
                "uri=" + uri +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", files=" + files +
                ", author=" + getAuthor() +
                ", dateCreated=" + getDateCreated() +
                '}';
    }
}
