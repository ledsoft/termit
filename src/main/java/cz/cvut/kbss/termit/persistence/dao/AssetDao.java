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
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.RecentlyModifiedAsset;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.List;

/**
 * Base DAO implementation for assets managed by the application.
 *
 * @param <T> Type of the asset
 */
public abstract class AssetDao<T extends Asset> extends BaseDao<T> {

    AssetDao(Class<T> type, EntityManager em) {
        super(type, em);
    }

    /**
     * Finds the specified number of most recently added/edited assets.
     *
     * @param limit Number of assets to load
     * @return List of recently added/edited assets
     */
    public List<RecentlyModifiedAsset> findLastEdited(int limit) {
        final List<RecentlyModifiedAsset> modified = em
                .createNativeQuery("SELECT DISTINCT ?entity ?label ?modified ?modifiedBy ?type WHERE {" +
                        "?entity a ?cls ;" +
                        "?dateCreated ?created ;" +
                        "rdfs:label ?label ;" +
                        "?createdBy ?author ." +
                        "OPTIONAL { " +
                        "?entity ?lastModified ?lastEdited ; " +
                        "?lastModifiedBy ?lastEditor ." +
                        "}" +
                        "BIND (COALESCE(?lastEdited, ?created) as ?modified)" +
                        "BIND (COALESCE(?lastEditor, ?author) as ?modifiedBy)" +
                        "BIND (?cls as ?type)" +
                        "} ORDER BY DESC(?modified) LIMIT ?limit", "RecentlyModifiedAsset")
                .setParameter("cls", typeUri)
                .setParameter("dateCreated", URI.create(Vocabulary.s_p_ma_datum_a_cas_vytvoreni))
                .setParameter("createdBy", URI.create(Vocabulary.s_p_ma_autora))
                .setParameter("lastModified", URI.create(Vocabulary.s_p_ma_datum_a_cas_posledni_modifikace))
                .setParameter("lastModifiedBy", URI.create(Vocabulary.s_p_ma_posledniho_editora))
                .setUntypedParameter("limit", limit).getResultList();
        loadLastEditors(modified);
        return modified;
    }

    private void loadLastEditors(List<RecentlyModifiedAsset> modified) {
        modified.forEach(m -> m.setEditor(em.find(User.class, m.getModifiedBy())));
    }
}
