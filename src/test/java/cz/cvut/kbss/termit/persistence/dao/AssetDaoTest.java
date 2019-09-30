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
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceDao sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    @Test
    void findRecentlyEditedLoadsSpecifiedCountOfRecentlyEditedResources() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<Resource> old = resources.subList(0, 5);
        final List<Resource> recent = resources.subList(5, resources.size());
        // We are setting the date here to work around the ProvenanceManager, which sets creation date on persist automatically
        transactional(() -> setOldCreated(old));

        final int count = 3;
        final List<Resource> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result));
    }

    @Test
    void findRecentlyEditedUsesLastModifiedDateWhenAvailable() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<Resource> recent = resources.subList(5, resources.size());
        transactional(() -> {
            setOldCreated(resources);
            recent.forEach(r -> {
                r.setDescription("Update");
                r.setLastModified(new Date());
                em.merge(r);
            });
        });
        em.getEntityManagerFactory().getCache().evictAll();

        final int count = 3;
        final List<Resource> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result));
    }

    private void setOldCreated(List<Resource> old) {
        final Repository repo = em.unwrap(Repository.class);
        final ValueFactory vf = repo.getValueFactory();
        try (final RepositoryConnection con = repo.getConnection()) {
            con.begin();
            old.forEach(r -> {
                con.remove(vf.createIRI(r.getUri().toString()), vf.createIRI(Vocabulary.s_p_ma_datum_a_cas_vytvoreni), null);
                con.add(vf.createIRI(r.getUri().toString()), vf.createIRI(Vocabulary.s_p_ma_datum_a_cas_vytvoreni),
                        vf.createLiteral(new Date(System.currentTimeMillis() - 24 * 3600 * 1000)));
            });
            con.commit();
        }
    }
}