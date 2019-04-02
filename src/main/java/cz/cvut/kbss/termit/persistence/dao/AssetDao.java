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
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Asset;
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
    public List<T> findLastEdited(int limit) {
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?dateCreated ?created ." +
                "OPTIONAL { ?x ?lastModified ?modified . }" +
                "BIND (IF (BOUND(?modified), ?modified, ?created) AS ?lastEdited)" +
                "} ORDER BY DESC(?lastEdited) LIMIT ?limit", type)
                 .setParameter("type", typeUri)
                 .setParameter("dateCreated", URI.create(Vocabulary.s_p_ma_datum_a_cas_vytvoreni))
                 .setParameter("lastModified", URI.create(Vocabulary.s_p_ma_datum_a_cas_posledni_modifikace))
                 .setUntypedParameter("limit", limit).getResultList();
    }
}
