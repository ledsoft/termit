package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;

@OWLClass(iri = cz.cvut.kbss.termit.util.Vocabulary.s_c_document_vocabulary)
public class DocumentVocabulary extends Vocabulary {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_describes, fetch = FetchType.EAGER)
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
