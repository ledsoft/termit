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
package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

/**
 * Represents basic data about a {@link Term}.
 * <p>
 * This is not a full blown entity and should not be used to modify data.
 */
@SparqlResultSetMapping(name = "TermInfo", classes = {@ConstructorResult(targetClass = TermInfo.class,
        variables = {
                @VariableResult(name = "entity", type = URI.class),
                @VariableResult(name = "label"),
                @VariableResult(name = "vocabulary", type = URI.class)
        })})
@OWLClass(iri = Vocabulary.s_c_term)
public class TermInfo implements Serializable {

    @Id
    private URI uri;

    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = SKOS.PREF_LABEL)
    private String label;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    public TermInfo() {
    }

    public TermInfo(URI uri) {
        this.uri = Objects.requireNonNull(uri);
    }

    // Constructor used by SparqlResultSetMapping
    public TermInfo(URI uri, String label, URI vocabulary) {
        this.uri = uri;
        this.label = label;
        this.vocabulary = vocabulary;
    }

    public TermInfo(Term term) {
        Objects.requireNonNull(term);
        this.uri = term.getUri();
        this.label = term.getLabel();
        this.vocabulary = term.getVocabulary();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public URI getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(URI vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TermInfo)) {
            return false;
        }
        TermInfo termInfo = (TermInfo) o;
        return Objects.equals(uri, termInfo.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "TermInfo{" + label + "<" + uri + ">" + '}';
    }
}
