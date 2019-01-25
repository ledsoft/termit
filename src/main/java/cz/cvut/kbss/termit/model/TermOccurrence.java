package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.termit.util.Vocabulary;

@OWLClass(iri = Vocabulary.s_c_vyskyt_termu)
public class TermOccurrence extends TermAssignment {

    public TermOccurrence() {
    }

    public TermOccurrence(Term term, OccurrenceTarget target) {
        super(term, target);
    }

    @Override
    public OccurrenceTarget getTarget() {
        assert target == null || target instanceof OccurrenceTarget;
        return (OccurrenceTarget) target;
    }

    public void setTarget(OccurrenceTarget target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "TermOccurrence - " + super.toString();
    }
}
