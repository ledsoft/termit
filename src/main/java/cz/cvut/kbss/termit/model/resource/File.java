package cz.cvut.kbss.termit.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.Inferred;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.Types;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_soubor)
public class File extends Resource {

    /**
     * File origin.
     * <p>
     * Can be used e.g. as baseURI for HTML files.
     */
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_puvod)
    private URI origin;

    @JsonIgnore
    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_je_casti_dokumentu)
    private Document document;

    @Types
    private Set<String> types;

    public File() {
        this.types = Collections.singleton(Vocabulary.s_c_zdroj);
    }

    public URI getOrigin() {
        return origin;
    }

    public void setOrigin(URI origin) {
        this.origin = origin;
    }

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
        File document = (File) o;
        return Objects.equals(getUri(), document.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "File{" +
                ", origin=" + origin +
                super.toString() + '}';
    }
}
