package cz.cvut.kbss.termit.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.model.util.SupportsStorage;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_soubor)
@EntityListeners(ProvenanceManager.class)
@JsonLdAttributeOrder({"uri", "label", "description", "author", "lastEditor"})
public class File extends Resource implements SupportsStorage {

    @JsonIgnore
    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_je_casti_dokumentu, fetch = FetchType.EAGER)
    private Document document;

    @Types
    private Set<String> types;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
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
        if (!(o instanceof File)) {
            return false;
        }
        File file = (File) o;
        return Objects.equals(getUri(), file.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "File{" +
                (document != null ? "document=<" + document.getUri() + '>' : "") +
                super.toString() + '}';
    }

    /**
     * Resolves the name of the directory corresponding to this file.
     * <p>
     * Note that two modes of operation exists for this method:
     * <ul>
     * <li>If parent document exists, the document's directory is returned</li>
     * <li>Otherwise, directory is resolved based on this file's label</li>
     * </ul>
     *
     * @return Name of the directory storing this file
     */
    @JsonIgnore
    @Override
    public String getDirectoryName() {
        if (document != null) {
            return document.getDirectoryName();
        } else {
            if (getLabel() == null || getUri() == null) {
                throw new IllegalStateException("Missing file name or URI required for directory name resolution.");
            }
            final String labelPart = getLabel().substring(0, getLabel().indexOf('.'));
            return IdentifierResolver.normalize(labelPart) + '_' + getUri().hashCode();
        }
    }
}
