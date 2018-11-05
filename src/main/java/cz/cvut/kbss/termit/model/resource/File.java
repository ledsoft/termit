package cz.cvut.kbss.termit.model.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.Inferred;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;

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

    @Override
    public String toString() {
        return "File{" +
                ", origin=" + origin +
                "} " + super.toString();
    }
}
