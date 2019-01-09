package cz.cvut.kbss.termit.model.resource;

import cz.cvut.kbss.jopa.model.annotations.EntityListeners;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.util.Objects;

@OWLClass(iri = Vocabulary.s_c_zdroj)
@EntityListeners(ProvenanceManager.class)
public class Resource extends Asset implements Serializable {

    @OWLDataProperty(iri = Vocabulary.s_p_description)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(getUri(), resource.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Resource{" +
                getLabel() +
                " <" + getUri() + '>' +
                "}";
    }
}
