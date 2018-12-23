package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.exception.UnsupportedAssetOperationException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Interface of business logic concerning resources.
 */
@Service
public class ResourceService implements AssetService<Resource> {

    private final ResourceRepositoryService repositoryService;

    @Autowired
    public ResourceService(ResourceRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
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
        // TODO
        return null;
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
}
