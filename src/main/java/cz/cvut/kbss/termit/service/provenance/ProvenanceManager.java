/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.provenance;

import cz.cvut.kbss.jopa.model.annotations.PostLoad;
import cz.cvut.kbss.jopa.model.annotations.PrePersist;
import cz.cvut.kbss.jopa.model.annotations.PreUpdate;
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
        instance.setCreated(new Date());
    }

    @PreUpdate
    void generateOnUpdate(HasProvenanceData instance) {
        assert instance != null;
        assert SecurityUtils.currentUser() != null;

        instance.setLastEditor(SecurityUtils.currentUser().toUser());
        instance.setLastModified(new Date());
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
