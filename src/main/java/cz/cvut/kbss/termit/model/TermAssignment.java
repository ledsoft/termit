package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_prirazeni_termu)
public class TermAssignment extends AbstractEntity implements HasTypes {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_je_prirazenim_termu, fetch = FetchType.EAGER)
    private Term term;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_cil, cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    Target target;

    @OWLDataProperty(iri = Vocabulary.s_p_description)
    private String description;

    @Types
    private Set<String> types;

    public Term getTerm() {
        return term;
    }

    public void setTerm(Term term) {
        this.term = term;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public String toString() {
        return "TermAssignment{" +
                "term=" + term +
                ", target=" + target +
                ", description='" + description + '\'' +
                ", types=" + types +
                "} " + super.toString();
    }
}
