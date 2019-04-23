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