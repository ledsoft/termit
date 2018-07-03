package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.CommonVocabulary;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_file)
public class File extends AbstractEntity {

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = CommonVocabulary.RDFS_LABEL)
    private String name;

    @OWLDataProperty(iri = Vocabulary.s_p_file_location)
    private String location;

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

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", location='" + location + '\'' +
                "} " + super.toString();
    }
}
