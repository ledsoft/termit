package cz.cvut.kbss.termit.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_dokument)
@EntityListeners(ProvenanceManager.class)
@JsonLdAttributeOrder({"uri", "label", "description", "author", "lastEditor"})
public class Document extends Resource {

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_soubor, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private Set<File> files;

    @JsonIgnore
    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_dokumentovy_slovnik, fetch = FetchType.EAGER)
    private DocumentVocabulary vocabulary;

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
        return files != null ? files.stream().filter(f -> f.getLabel().equals(fileName)).findAny() :
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
        if (getLabel() == null || getUri() == null) {
            throw new IllegalStateException("Missing document name or URI required for directory name resolution.");
        }
        return IdentifierResolver.normalize(getLabel()) + "_" + getUri().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document)) {
            return false;
        }
        Document document = (Document) o;
        return Objects.equals(getUri(), document.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Document{" +
                "uri=" + getUri() +
                ", name='" + getLabel() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", files=" + files +
                ", author=" + getAuthor() +
                ", dateCreated=" + getCreated() +
                '}';
    }

    public static Field getFilesField() {
        try {
            return Document.class.getDeclaredField("files");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"files\" field.", e);
        }
    }
}
