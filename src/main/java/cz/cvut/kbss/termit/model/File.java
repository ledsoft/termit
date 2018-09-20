package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.net.URI;

@OWLClass(iri = Vocabulary.s_c_soubor)
public class File extends AbstractEntity {

    @OWLDataProperty(iri = RDFS.COMMENT)
    private String comment;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_nazev_souboru)
    private String fileName;

    /**
     * File origin.
     * <p>
     * Can be used e.g. as baseURI for HTML files.
     */
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_puvod)
    private URI origin;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public URI getOrigin() {
        return origin;
    }

    public void setOrigin(URI origin) {
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "File{" +
                "fileName='" + fileName + '\'' +
                ", origin=" + origin +
                "} " + super.toString();
    }
}
