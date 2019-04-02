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
package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.exception.UnsupportedAssetOperationException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.document.DocumentManager;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface of business logic concerning resources.
 */
@Service
public class ResourceService implements CrudService<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepositoryService repositoryService;

    private final DocumentManager documentManager;

    private final TextAnalysisService textAnalysisService;

    @Autowired
    public ResourceService(ResourceRepositoryService repositoryService, DocumentManager documentManager,
                           TextAnalysisService textAnalysisService) {
        this.repositoryService = repositoryService;
        this.documentManager = documentManager;
        this.textAnalysisService = textAnalysisService;
    }

    /**
     * Sets terms with which the specified target resource is annotated.
     *
     * @param target   Target resource
     * @param termUris Identifiers of terms annotating the resource
     */
    @Transactional
    public void setTags(Resource target, Collection<URI> termUris) {
        repositoryService.setTags(target, termUris);
    }

    /**
     * Removes the specified resource.
     * <p>
     * Resource removal also involves cleanup of annotations and term occurrences associated with it.
     * <p>
     * TODO: Pull remove method up into AssetService once removal is supported by other types of assets as well
     *
     * @param resource Resource to remove
     */
    @Transactional
    public void remove(Resource resource) {
        repositoryService.remove(resource);
    }

    /**
     * Gets terms the specified resource is annotated with.
     *
     * @param resource Annotated resource
     * @return List of terms annotating the specified resource
     */
    public List<Term> findTags(Resource resource) {
        return repositoryService.findTags(resource);
    }

    /**
     * Gets term assignments related to the specified resource.
     * <p>
     * This includes both assignments and occurrences.
     *
     * @param resource Target resource
     * @return List of term assignments and occurrences
     */
    public List<TermAssignment> findAssignments(Resource resource) {
        return repositoryService.findAssignments(resource);
    }

    /**
     * Finds resources which are related to the specified one.
     * <p>
     * Two resources are related in this scenario if they have at least one common term assigned to them.
     *
     * @param resource Resource to filter by
     * @return List of resources related to the specified one
     */
    public List<Resource> findRelated(Resource resource) {
        return repositoryService.findRelated(resource);
    }

    /**
     * Gets content of the specified resource.
     *
     * @param resource Resource whose content should be retrieved
     * @return Representation of the resource content
     * @throws UnsupportedAssetOperationException When content of the specified resource cannot be retrieved
     */
    public TypeAwareResource getContent(Resource resource) {
        Objects.requireNonNull(resource);
        if (!(resource instanceof File)) {
            throw new UnsupportedAssetOperationException("Content retrieval is not supported for resource " + resource);
        }
        return documentManager.getAsResource((File) resource);
    }

    /**
     * Saves content of the specified resource.
     *
     * @param resource Domain resource associated with the content
     * @param content  Resource content
     * @throws UnsupportedAssetOperationException If content saving is not supported for the specified resource
     */
    @Transactional
    public void saveContent(Resource resource, InputStream content) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(content);
        if (!(resource instanceof File)) {
            throw new UnsupportedAssetOperationException("Content saving is not supported for resource " + resource);
        }
        LOG.trace("Saving new content of resource {}.", resource);
        final File file = (File) resource;
        if (documentManager.exists(file)) {
            documentManager.createBackup(file);
        }
        documentManager.saveFileContent(file, content);
    }

    /**
     * Executes text analysis on the specified resource's content.
     *
     * @param resource Resource to analyze
     * @throws UnsupportedAssetOperationException If text analysis is not supported for the specified resource
     */
    public void runTextAnalysis(Resource resource) {
        Objects.requireNonNull(resource);
        if (!(resource instanceof File)) {
            throw new UnsupportedAssetOperationException("Text analysis is not supported for resource " + resource);
        }
        LOG.trace("Invoking text analysis on resource {}.", resource);
        textAnalysisService.analyzeFile((File) resource);
    }

    @Override
    public List<Resource> findAll() {
        return repositoryService.findAll();
    }

    @Override
    public Optional<Resource> find(URI id) {
        return repositoryService.find(id);
    }

    @Override
    public Resource findRequired(URI id) {
        return repositoryService.findRequired(id);
    }

    @Override
    public Optional<Resource> getReference(URI id) {
        return repositoryService.getReference(id);
    }

    @Override
    public Resource getRequiredReference(URI id) {
        return repositoryService.getRequiredReference(id);
    }

    @Override
    public boolean exists(URI id) {
        return repositoryService.exists(id);
    }

    @Transactional
    @Override
    public void persist(Resource instance) {
        repositoryService.persist(instance);
    }

    @Transactional
    @Override
    public Resource update(Resource instance) {
        return repositoryService.update(instance);
    }

    /**
     * Generates a resource identifier based on the specified label.
     *
     * @param label Resource label
     * @return Resource identifier
     */
    public URI generateIdentifier(String label) {
        return repositoryService.generateIdentifier(label);
    }
}
