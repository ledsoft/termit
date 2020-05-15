package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.workspace.WorkspaceNotSetException;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.persistence.dao.WorkspaceDao;
import cz.cvut.kbss.termit.service.business.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.net.URI;

@Service
public class WorkspaceRepositoryService implements WorkspaceService {

    static final String WORKSPACE_ATT = "workspace";

    private final HttpSession session;

    private final WorkspaceDao workspaceDao;

    @Autowired
    public WorkspaceRepositoryService(HttpSession session, WorkspaceDao workspaceDao) {
        this.session = session;
        this.workspaceDao = workspaceDao;
    }

    @Override
    public Workspace loadWorkspace(URI id) {
        final Workspace ws = workspaceDao.find(id).orElseThrow(
                () -> NotFoundException.create(Workspace.class.getSimpleName(), id));
        session.setAttribute(WORKSPACE_ATT, ws);
        return ws;
    }

    @Override
    public Workspace loadCurrentWorkspace() {
        // TODO
        return null;
    }

    @Override
    public Workspace getCurrentWorkspace() {
        final Object workspace = session.getAttribute(WORKSPACE_ATT);
        if (workspace == null) {
            throw new WorkspaceNotSetException("No workspace is currently selected!");
        }
        return (Workspace) workspace;
    }
}
