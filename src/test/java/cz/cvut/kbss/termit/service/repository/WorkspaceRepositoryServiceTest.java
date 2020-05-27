package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.workspace.WorkspaceNotSetException;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;

import static cz.cvut.kbss.termit.service.repository.WorkspaceRepositoryService.WORKSPACE_ATT;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private HttpSession session;

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

    @Test
    void loadWorkspaceByIdStoresLoadedWorkspaceInSession() {
        final Workspace expected = generateWorkspace();
        sut.loadWorkspace(expected.getUri());
        assertEquals(expected, session.getAttribute(WORKSPACE_ATT));
    }

    @Test
    void getCurrentWorkspaceRetrievesCurrentWorkspaceFromSession() {
        final Workspace expected = generateWorkspace();
        session.setAttribute(WORKSPACE_ATT, expected);
        assertEquals(expected, sut.getCurrentWorkspace());
    }

    @Test
    void getCurrentWorkspaceThrowsWorkspaceNotSetExceptionWhenNoWorkspaceIsSelected() {
        assertNull(session.getAttribute(WORKSPACE_ATT));
        assertThrows(WorkspaceNotSetException.class, () -> sut.getCurrentWorkspace());
    }
}
