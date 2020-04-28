/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.resource.Document;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.util.Objects;

@OWLClass(iri = cz.cvut.kbss.termit.util.Vocabulary.s_c_dokumentovy_slovnik)
@JsonLdAttributeOrder({"uri", "label", "comment"})
public class DocumentVocabulary extends Vocabulary {

    @NotNull
    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_popisuje_dokument, cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
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

    public static Field getDocumentField() {
        try {
            return DocumentVocabulary.class.getDeclaredField("document");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"document\" field.", e);
        }
    }
}
