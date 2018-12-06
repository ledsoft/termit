package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.jopa.model.annotations.PrePersist;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.service.security.SecurityUtils;

import java.util.Date;

/**
 * Entity listener used to generate provenance data for instances being persisted or updated.
 */
public class ProvenanceGenerator {

    /**
     * Sets provenance data (author, datetime of creation) of the specified instance.
     *
     * @param instance Instance being persisted for which provenance data will be generated
     */
    @PrePersist
    void prePersist(HasProvenanceData instance) {
        assert instance != null;
        assert SecurityUtils.currentUser() != null;

        instance.setAuthor(SecurityUtils.currentUser().toUser());
        instance.setDateCreated(new Date());
    }
}
