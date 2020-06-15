package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.dto.workspace.VocabularyInfo;
import cz.cvut.kbss.termit.dto.workspace.WorkspaceMetadata;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.workspace.WorkspaceMetadataCache;
import cz.cvut.kbss.termit.persistence.dao.WorkspaceDao;
import cz.cvut.kbss.termit.service.business.WorkspaceService;
import cz.cvut.kbss.termit.workspace.WorkspaceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceRepositoryService implements WorkspaceService {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceRepositoryService.class);

    private final WorkspaceDao workspaceDao;

    private final WorkspaceStore workspaceStore;

    private final WorkspaceMetadataCache workspaceCache;

    @Autowired
    public WorkspaceRepositoryService(WorkspaceDao workspaceDao, WorkspaceStore workspaceStore,
                                      WorkspaceMetadataCache workspaceCache) {
        this.workspaceDao = workspaceDao;
        this.workspaceStore = workspaceStore;
        this.workspaceCache = workspaceCache;
    }

    @Override
    public Workspace loadWorkspace(URI id) {
        LOG.trace("Loading workspace {}.", id);
        final Workspace ws = workspaceDao.find(id).orElseThrow(
                () -> NotFoundException.create(Workspace.class.getSimpleName(), id));
        LOG.trace("Storing workspace ID in session.");
        workspaceStore.setCurrentWorkspace(id);
        workspaceCache.putWorkspace(loadWorkspaceMetadata(ws));
        return ws;
    }

    private WorkspaceMetadata loadWorkspaceMetadata(Workspace ws) {
        final WorkspaceMetadata metadata = new WorkspaceMetadata(ws);
        final List<VocabularyInfo> vocabularies = workspaceDao.findWorkspaceVocabularyMetadata(ws);
        metadata.setVocabularies(vocabularies.stream().collect(Collectors.toMap(VocabularyInfo::getUri, vi -> vi)));
        return metadata;
    }

    @Override
    public Workspace loadCurrentWorkspace() {
        // TODO
        return null;
    }

    @Override
    public Workspace getCurrentWorkspace() {
        return workspaceCache.getCurrentWorkspace();
    }
}
