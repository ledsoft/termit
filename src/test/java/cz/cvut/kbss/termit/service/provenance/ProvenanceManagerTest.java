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
package cz.cvut.kbss.termit.service.provenance;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ProvenanceManagerTest {

    private ProvenanceManager sut = new ProvenanceManager();

    @AfterEach
    void tearDown() {
        Environment.resetCurrentUser();
    }

    @Test
    void prePersistSetsAuthorOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.generateOnPersist(entity);
        assertNotNull(entity.getAuthor());
        assertEquals(ua.toUser(), entity.getAuthor());
    }

    @Test
    void prePersistSetsDateCreatedOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.generateOnPersist(entity);
        assertNotNull(entity.getCreated());
    }

    @Test
    void preUpdateSetsEditorOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.generateOnUpdate(entity);
        assertNotNull(entity.getLastEditor());
        assertEquals(ua.toUser(), entity.getLastEditor());
    }

    @Test
    void preUpdateSetsDateEditedOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.generateOnUpdate(entity);
        assertNotNull(entity.getLastModified());
    }

    @Test
    void postLoadClearsAuthorDataForAnonymousAccess() {
        final Resource entity = Generator.generateResourceWithId();
        final UserAccount ua = Generator.generateUserAccount();
        entity.setAuthor(ua.toUser());
        entity.setCreated(new Date());

        sut.clearForAnonymousOnLoad(entity);
        assertNull(entity.getAuthor());
        assertNotNull(entity.getCreated());
    }

    @Test
    void postLoadDoesNothingWhenUserIsLoggedIn() {
        final Resource entity = Generator.generateResourceWithId();
        final UserAccount ua = Generator.generateUserAccount();
        entity.setAuthor(ua.toUser());
        entity.setLastEditor(ua.toUser());
        entity.setCreated(new Date());
        Environment.setCurrentUser(Generator.generateUserAccount());

        sut.clearForAnonymousOnLoad(entity);
        assertNotNull(entity.getAuthor());
        assertNotNull(entity.getCreated());
        assertNotNull(entity.getLastEditor());
    }
    
    @Test
    void postLoadClearsLastEditorDataForAnonymousAccess() {
        final Resource entity = Generator.generateResourceWithId();
        final UserAccount ua = Generator.generateUserAccount();
        entity.setAuthor(ua.toUser());
        entity.setLastEditor(ua.toUser());
        entity.setCreated(new Date());

        sut.clearForAnonymousOnLoad(entity);
        assertNull(entity.getAuthor());
        assertNull(entity.getLastEditor());
    }
}