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
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.changetracking.PersistChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.UpdateChangeRecord;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.time.Instant;
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
        final List<PersistChangeRecord> persistRecords = resources.stream().map(Generator::generatePersistChange)
                                                                  .collect(Collectors.toList());
        setOldCreated(persistRecords.subList(0, 5));
        final List<URI> recent = resources.subList(5, resources.size()).stream().map(Resource::getUri)
                                          .collect(Collectors.toList());
        transactional(() -> persistRecords.forEach(em::persist));

        final int count = 3;
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recent.containsAll(result.stream().map(RecentlyModifiedAsset::getUri).collect(Collectors.toList())));
    }

    @Test
    void findRecentlyEditedUsesLastModifiedDateWhenAvailable() {
        final List<Resource> resources = IntStream.range(0, 10).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        transactional(() -> resources.forEach(em::persist));
        final List<PersistChangeRecord> persistRecords = resources.stream().map(Generator::generatePersistChange)
                                                                  .collect(Collectors.toList());
        setOldCreated(persistRecords);
        final List<Resource> recent = resources.subList(5, resources.size());
        final List<UpdateChangeRecord> updateRecords = recent.stream().map(Generator::generateUpdateChange)
                                                             .collect(Collectors.toList());
        transactional(() -> {
            persistRecords.forEach(em::persist);
            updateRecords.forEach(em::persist);
        });
        em.getEntityManagerFactory().getCache().evictAll();

        final int count = 3;
        final List<URI> recentUris = recent.stream().map(Resource::getUri).collect(Collectors.toList());
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(recentUris
                .containsAll(result.stream().map(RecentlyModifiedAsset::getUri).collect(Collectors.toList())));
    }

    private void setOldCreated(List<PersistChangeRecord> old) {
        old.forEach(r -> r.setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis() - 24 * 3600 * 1000)));
    }
}
