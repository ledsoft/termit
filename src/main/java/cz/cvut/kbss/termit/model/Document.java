package cz.cvut.kbss.termit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
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

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_soubor, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private Set<File> files;

    @JsonIgnore
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

    /**
     * Retrieves file with the specified filename from this document.
     *
     * @param fileName Name of the file to retrieve
     * @return {@code Optional} containing the file with a matching filename or an empty {@code Optional}
     */
    public Optional<File> getFile(String fileName) {
        return files != null ? files.stream().filter(f -> f.getFileName().equals(fileName)).findAny() :
               Optional.empty();
    }

    public DocumentVocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(DocumentVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets name of the directory where files comprising this document are stored.
     * <p>
     * The name consists of normalized name of this document, appended with hash code of this document's URI.
     * <p>
     * Note that the full directory path consists of the configured storage directory ({@link
     * cz.cvut.kbss.termit.util.ConfigParam#FILE_STORAGE}) to which the document-specific directory name is appended.
     *
     * @return Document-specific directory name
     */
    public String getFileDirectoryName() {
        if (name == null || uri == null) {
            throw new IllegalStateException("Missing document name or URI required for directory name resolution.");
        }
        return IdentifierResolver.normalize(name) + "_" + uri.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;
        Document document = (Document) o;
        return Objects.equals(uri, document.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
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
