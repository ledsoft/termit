package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Interface of business logic concerning resources.
 */
public interface ResourceService extends AssetService<Resource> {

    /**
     * Sets terms with which the specified target resource is annotated.
     *
     * @param target   Target resource
     * @param termUris Identifiers of terms annotating the resource
     */
    void setTags(Resource target, Collection<URI> termUris);

    /**
     * Removes the specified resource.
     * <p>
     * Resource removal also involves cleanup of annotations and term occurrences associated with it.
     * <p>
     * TODO: Pull remove method up into AssetService once removal is supported by other types of assets as well
     *
     * @param resource Resource to remove
     */
    void remove(Resource resource);

    /**
     * Gets terms the specified resource is annotated with.
     *
     * @param resource Annotated resource
     * @return List of terms annotating the specified resource
     */
    List<Term> findTags(Resource resource);

    /**
     * Finds resources which are related to the specified one.
     * <p>
     * Two resources are related in this scenario if they have at least one common term assigned to them.
     *
     * @param resource Resource to filter by
     * @return List of resources related to the specified one
     */
    List<Resource> findRelated(Resource resource);
}
