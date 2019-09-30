/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_prirazeni_termu)
public class TermAssignment extends AbstractEntity implements HasTypes {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_je_prirazenim_termu)
    private URI term;

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_cil, cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    Target target;

    @OWLDataProperty(iri = DC.Terms.DESCRIPTION)
    private String description;

    @Types
    private Set<String> types;

    public TermAssignment() {
    }

    public TermAssignment(URI termUri, Target target) {
        this.term = Objects.requireNonNull(termUri);
        this.target = Objects.requireNonNull(target);
    }

    public URI getTerm() {
        return term;
    }

    public void setTerm(URI term) {
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
                "term=<" + term + ">" +
                ", target=" + target +
                ", description='" + description + '\'' +
                ", types=" + types +
                "} " + super.toString();
    }
}
