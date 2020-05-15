package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private WorkspaceRepositoryService sut;

    @Test
    void loadWorkspaceByIdRetrievesWorkspaceFromRepository() {
        final Workspace expected = generateWorkspace();
        final Workspace result = sut.loadWorkspace(expected.getUri());
        assertEquals(expected, result);
    }

    private Workspace generateWorkspace() {
        final Workspace workspace = new Workspace();
        workspace.setUri(Generator.generateUri());
        workspace.setLabel("Test workspace " + Generator.randomInt(0, 1000));
        transactional(() -> em.persist(workspace, new EntityDescriptor(workspace.getUri())));
        return workspace;
    }

    @Test
    void loadWorkspaceByIdThrowsNotFoundExceptionWhenWorkspaceDoesNotExist() {
        assertThrows(NotFoundException.class, () -> sut.loadWorkspace(Generator.generateUri()));
    }
}
