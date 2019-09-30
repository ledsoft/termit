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
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_cil_vyskytu)
public class OccurrenceTarget extends Target {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_selektor_termu, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<TermSelector> selectors;

    public OccurrenceTarget() {
    }

    public OccurrenceTarget(File source) {
        super(source);
    }

    public Set<TermSelector> getSelectors() {
        return selectors;
    }

    public void setSelectors(Set<TermSelector> selectors) {
        this.selectors = selectors;
    }

    @Override
    public String toString() {
        return "OccurrenceTarget{" +
                "selectors=" + selectors +
                "} " + super.toString();
    }
}
