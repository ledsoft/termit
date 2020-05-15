package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

@Repository
public class WorkspaceDao {

    private final EntityManager em;

    @Autowired
    public WorkspaceDao(EntityManager em) {
        this.em = em;
    }

    /**
     * Finds workspace with the specified identifier.
     *
     * @param id Workspace identifier
     * @return {@link Optional} containing the loaded workspace, or an empty optional if the workspace is not found.
     */
    public Optional<Workspace> find(URI id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(em.find(Workspace.class, id));
    }

    /**
     * Finds the specified user's current workspace.
     *
     * @param user User for which workspace should be retrieved
     * @return Current workspace of the specified user (if it is set)
     * @see #setCurrentWorkspace(UserAccount, Workspace)
     */
    public Optional<Workspace> findCurrentForUser(UserAccount user) {
        // TODO
        return Optional.empty();
    }

    /**
     * Saves the specified workspace as current for the specified user.
     * <p>
     * This information may be useful for later retrieval (e.g., returning to the application).
     *
     * @param user      User
     * @param workspace Workspace to set as current for the user
     * @see #findCurrentForUser(UserAccount)
     */
    public void setCurrentWorkspace(UserAccount user, Workspace workspace) {
        // TODO
    }
}
