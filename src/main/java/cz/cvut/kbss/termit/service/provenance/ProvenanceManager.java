package cz.cvut.kbss.termit.service.provenance;

import cz.cvut.kbss.jopa.model.annotations.PostLoad;
import cz.cvut.kbss.jopa.model.annotations.PrePersist;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.service.security.SecurityUtils;

import java.util.Date;

/**
 * Entity listener used to manage provenance data.
 */
public class ProvenanceManager {

    /**
     * Sets provenance data (author, datetime of creation) of the specified instance.
     *
     * @param instance Instance being persisted for which provenance data will be generated
     */
    @PrePersist
    void generateOnPersist(HasProvenanceData instance) {
        assert instance != null;
        assert SecurityUtils.currentUser() != null;

        instance.setAuthor(SecurityUtils.currentUser().toUser());
        instance.setDateCreated(new Date());
    }

    /**
     * Clears author data after instance load in case of anonymous access, i.e., when no user is authenticated.
     *
     * @param instance Loaded instance
     */
    @PostLoad
    void clearForAnonymousOnLoad(HasProvenanceData instance) {
        assert instance != null;

        if (!SecurityUtils.authenticated()) {
            instance.setAuthor(null);
        }
    }
}
