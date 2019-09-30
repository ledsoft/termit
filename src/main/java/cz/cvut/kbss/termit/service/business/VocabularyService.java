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
package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Vocabulary;

import java.net.URI;
import java.util.Collection;

/**
 * Interface of business logic concerning vocabularies.
 */
public interface VocabularyService extends CrudService<Vocabulary> {

    /**
     * Generates a vocabulary identifier based on the specified label.
     *
     * @param label Vocabulary label
     * @return Vocabulary identifier
     */
    URI generateIdentifier(String label);

    /**
     * Gets identifiers of all vocabularies imported by the specified vocabulary, including transitively imported ones.
     *
     * @param entity Base vocabulary, whose imports should be retrieved
     * @return Collection of (transitively) imported vocabularies
     */
    Collection<URI> getTransitivelyImportedVocabularies(Vocabulary entity);
}
