package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.model.resource.Document;

import javax.validation.constraints.NotNull;

@OWLClass(iri = cz.cvut.kbss.termit.util.Vocabulary.s_c_dokumentovy_slovnik)
public class DocumentVocabulary extends Vocabulary {

    @NotNull
    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_popisuje_dokument, fetch = FetchType.EAGER)
    private Document document;

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "DocumentVocabulary{" +
                "document=" + document +
                "} " + super.toString();
    }
}
