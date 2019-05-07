package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;

/**
 * Targets are used to denote which resources are assigned a term.
 */
@OWLClass(iri = Vocabulary.s_c_cil)
public class Target extends AbstractEntity {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_zdroj, fetch = FetchType.EAGER)
    private URI source;

    public Target() {
    }

    public Target(Resource source) {
        this.source = Objects.requireNonNull(source).getUri();
    }

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "Target{" +
                ", source=<" + source + ">" +
                "} " + super.toString();
    }
}
