package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.model.resource.Document;

import javax.validation.constraints.NotNull;
import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentVocabulary)) {
            return false;
        }
        DocumentVocabulary that = (DocumentVocabulary) o;
        return Objects.equals(getUri(), that.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "DocumentVocabulary{" +
                "document=" + document +
                "} " + super.toString();
    }
}
