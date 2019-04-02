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
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermOccurrenceDao extends BaseDao<TermOccurrence> {

    @Autowired
    public TermOccurrenceDao(EntityManager em) {
        super(TermOccurrence.class, em);
    }

    /**
     * Finds all occurrences of the specified term.
     *
     * @param term Term whose occurrences should be returned
     * @return List of term occurrences
     */
    public List<TermOccurrence> findAll(Term term) {
        Objects.requireNonNull(term);
        return em.createNativeQuery("SELECT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTerm ?term . }", TermOccurrence.class)
                 .setParameter("type", typeUri)
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("term", term.getUri()).getResultList();
    }

    /**
     * Finds all term occurrences which have at least one target pointing to the specified resource.
     * <p>
     * I.e., these term occurrences appear in the specified resource (presumably file).
     *
     * @param resource Resource to filter by
     * @return List of matching term occurrences
     */
    public List<TermOccurrence> findAll(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT DISTINCT ?x WHERE {" +
                "?x a ?type ;" +
                "?hasTarget ?target ." +
                "?target ?hasSource ?resource . }", TermOccurrence.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }
}
