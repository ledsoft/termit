package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_vyskyt_termu)
public class TermOccurrence extends AbstractEntity {

    @OWLDataProperty(iri = Vocabulary.s_p_description)
    private String description;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_je_vyskytem_termu, fetch = FetchType.EAGER)
    private Term term;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_cil, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Target> targets;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Term getTerm() {
        return term;
    }

    public void setTerm(Term term) {
        this.term = term;
    }

    public Set<Target> getTargets() {
        return targets;
    }

    public void setTargets(Set<Target> targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "TermOccurrence{" +
                "description='" + description + '\'' +
                ", term=" + term +
                "target count=" + (targets != null ? targets.size() : 0) +
                "} " + super.toString();
    }
}
