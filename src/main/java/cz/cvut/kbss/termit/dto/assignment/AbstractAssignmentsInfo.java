package cz.cvut.kbss.termit.dto.assignment;

import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.Types;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

abstract class AbstractAssignmentsInfo implements HasTypes {

    @OWLObjectProperty(iri = Vocabulary.s_p_je_prirazenim_termu)
    private URI term;

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_zdroj)
    private URI resource;

    @Types
    private Set<String> types;

    public AbstractAssignmentsInfo() {
    }

    protected AbstractAssignmentsInfo(URI term, URI resource) {
        this.term = term;
        this.resource = resource;
    }

    public URI getTerm() {
        return term;
    }

    public void setTerm(URI term) {
        this.term = term;
    }

    public URI getResource() {
        return resource;
    }

    public void setResource(URI resource) {
        this.resource = resource;
    }

    @Override
    public Set<String> getTypes() {
        return types;
    }

    @Override
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractAssignmentsInfo)) {
            return false;
        }
        AbstractAssignmentsInfo that = (AbstractAssignmentsInfo) o;
        return Objects.equals(term, that.term) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, resource, types);
    }

    @Override
    public String toString() {
        return "term=" + term +
                ", resource=" + resource +
                ", types=" + types;
    }
}
