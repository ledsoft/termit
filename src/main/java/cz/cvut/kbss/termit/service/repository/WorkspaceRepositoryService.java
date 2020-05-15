package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.persistence.dao.WorkspaceDao;
import cz.cvut.kbss.termit.service.business.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class WorkspaceRepositoryService implements WorkspaceService {

    private final WorkspaceDao workspaceDao;

    @Autowired
    public WorkspaceRepositoryService(WorkspaceDao workspaceDao) {
        this.workspaceDao = workspaceDao;
    }

    @Override
    public Workspace loadWorkspace(URI id) {
        return workspaceDao.find(id).orElseThrow(() -> NotFoundException.create(Workspace.class.getSimpleName(), id));
    }

    @Override
    public Workspace loadCurrentWorkspace() {
        // TODO
        return null;
    }

    @Override
    public Workspace getCurrentWorkspace() {
        // TODO
        return null;
    }
}
