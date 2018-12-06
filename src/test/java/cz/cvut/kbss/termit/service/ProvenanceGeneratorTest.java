package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProvenanceGeneratorTest {

    private ProvenanceGenerator sut = new ProvenanceGenerator();

    @Test
    void prePersistSetsAuthorOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.prePersist(entity);
        assertNotNull(entity.getAuthor());
        assertEquals(ua.toUser(), entity.getAuthor());
    }

    @Test
    void prePersistSetsDateCreatedOnSpecifiedEntity() {
        final UserAccount ua = Generator.generateUserAccount();
        Environment.setCurrentUser(ua);
        final Resource entity = Generator.generateResourceWithId();

        sut.prePersist(entity);
        assertNotNull(entity.getDateCreated());
    }
}