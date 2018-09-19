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

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = RDFS.LABEL)
    private String name;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_umisteni)
    private String location;

    /**
     * File origin.
     * <p>
     * Can be used e.g. as baseURI for HTML files.
     */
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_puvod)
    private URI origin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
                "name='" + name + '\'' +
                ", location='" + location + '\'' +
                "} " + super.toString();
    }
}
