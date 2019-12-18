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
package cz.cvut.kbss.termit.model.resource;

import cz.cvut.kbss.jopa.model.annotations.EntityListeners;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.asset.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.util.Objects;

@OWLClass(iri = Vocabulary.s_c_zdroj)
@EntityListeners(ProvenanceManager.class)
@JsonLdAttributeOrder({"uri", "label", "description", "author", "lastEditor"})
public class Resource extends Asset implements Serializable {

    @OWLDataProperty(iri = DC.Terms.DESCRIPTION)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(getUri(), resource.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Resource{" +
                getLabel() +
                " <" + getUri() + '>' +
                "}";
    }
}
