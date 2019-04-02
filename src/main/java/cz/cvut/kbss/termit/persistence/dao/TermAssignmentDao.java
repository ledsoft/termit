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
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Repository
public class TermAssignmentDao extends BaseDao<TermAssignment> {

    @Autowired
    public TermAssignmentDao(EntityManager em) {
        super(TermAssignment.class, em);
    }

    /**
     * Finds all assignments of the specified terms.
     *
     * @param term Term whose assignments should be returned
     * @return List of matching assignments
     */
    public List<TermAssignment> findAll(Term term) {
        Objects.requireNonNull(term);
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasTerm ?term . }",
                TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTerm", URI.create(Vocabulary.s_p_je_prirazenim_termu))
                 .setParameter("term", term.getUri()).getResultList();
    }

    public List<TermAssignment> findByTarget(Target target) {
        Objects.requireNonNull(target);
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasTarget ?target. }",
                TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("target", target.getUri()).getResultList();
    }

    /**
     * Finds all assignments whose target represents this resource.
     * <p>
     * This includes both term assignments and term occurrences.
     *
     * @param resource Target resource to filter by
     * @return List of matching assignments
     */
    public List<TermAssignment> findAll(Resource resource) {
        Objects.requireNonNull(resource);
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasTarget/?hasSource ?resource. }",
                TermAssignment.class).setParameter("type", typeUri)
                 .setParameter("hasTarget", URI.create(Vocabulary.s_p_ma_cil))
                 .setParameter("hasSource", URI.create(Vocabulary.s_p_ma_zdroj))
                 .setParameter("resource", resource.getUri()).getResultList();
    }
}
