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

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.lang.reflect.Field;
import java.util.Date;

@MappedSuperclass
public abstract class HasProvenanceData {

    @ParticipationConstraints(nonEmpty = true)
    @OWLObjectProperty(iri = Vocabulary.s_p_ma_autora, fetch = FetchType.EAGER)
    private User author;

    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_datum_a_cas_vytvoreni)
    private Date created;

    @OWLObjectProperty(iri = Vocabulary.s_p_ma_posledniho_editora, fetch = FetchType.EAGER)
    private User lastEditor;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_datum_a_cas_posledni_modifikace)
    private Date lastModified;

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public User getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(User lastEditor) {
        this.lastEditor = lastEditor;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the datetime of this asset's last modification.
     * <p>
     * If {@link #getLastModified()} is set, its value is returned. Otherwise, {@link #getCreated()} is returned.
     *
     * @return Datetime of the last modification, if present, or creation of this asset
     */
    @JsonIgnore
    public Date getLastModifiedOrCreated() {
        return lastModified != null ? lastModified : created;
    }

    public static Field getAuthorField() {
        try {
            return HasProvenanceData.class.getDeclaredField("author");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"author\" field.", e);
        }
    }

    public static Field getLastEditorField() {
        try {
            return HasProvenanceData.class.getDeclaredField("lastEditor");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"lastEditor\" field.", e);
        }
    }
}
