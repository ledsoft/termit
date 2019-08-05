package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.dto.assignment.ResourceTermAssignments;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.ResourceDao;
import cz.cvut.kbss.termit.persistence.dao.TargetDao;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class ResourceRepositoryService extends BaseAssetRepositoryService<Resource> {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceRepositoryService.class);

    private final ResourceDao resourceDao;
    private final TargetDao targetDao;
    private final TermOccurrenceDao termOccurrenceDao;

    private final TermAssignmentRepositoryService assignmentService;
    private final VocabularyRepositoryService vocabularyService;

    private final IdentifierResolver idResolver;

    @Autowired
    public ResourceRepositoryService(Validator validator, ResourceDao resourceDao,
                                     TargetDao targetDao,
                                     TermOccurrenceDao termOccurrenceDao,
                                     TermAssignmentRepositoryService assignmentService,
                                     VocabularyRepositoryService vocabularyService,
                                     IdentifierResolver idResolver) {
        super(validator);
        this.resourceDao = resourceDao;
        this.targetDao = targetDao;
        this.termOccurrenceDao = termOccurrenceDao;
        this.vocabularyService = vocabularyService;
        this.assignmentService = assignmentService;
        this.idResolver = idResolver;
    }

    @Override
    protected AssetDao<Resource> getPrimaryDao() {
        return resourceDao;
    }

    @Override
    protected void prePersist(Resource instance) {
        super.prePersist(instance);
        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(instance.getLabel()));
        }
        verifyIdentifierUnique(instance);
    }

    /**
     * Persists the specified Resource in the context of the specified Vocabulary.
     *
     * @param resource   Resource to persist
     * @param vocabulary Vocabulary context
     * @throws IllegalArgumentException If the specified Resource is neither a {@code Document} nor a {@code File}
     */
    @Transactional
    public void persist(Resource resource, Vocabulary vocabulary) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(vocabulary);
        prePersist(resource);
        resourceDao.persist(resource, vocabulary);
    }

    /**
     * Updates the specified Resource in the context of the specified Vocabulary.
     *
     * @param resource   Resource to update
     * @param vocabulary Vocabulary context
     * @throws IllegalArgumentException If the specified Resource is neither a {@code Document} nor a {@code File}
     */
    @Transactional
    public Resource update(Resource resource, Vocabulary vocabulary) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(vocabulary);
        preUpdate(resource);
        final Resource result = resourceDao.update(resource, vocabulary);
        assert result != null;
        postUpdate(resource);
        return result;
    }

    /**
     * Gets terms the specified resource is annotated with.
     *
     * @param resource Annotated resource
     * @return List of terms annotating the specified resource
     */
    public List<Term> findTags(Resource resource) {
        return resourceDao.findTerms(resource);
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
        return assignmentService.findAll(resource);
    }

    /**
     * Gets aggregated information about Terms assigned to the specified Resource.
     * <p>
     * Since retrieving all the assignments and occurrences related to the specified Resource may be time consuming and
     * is rarely required, this method provides aggregate information in that the returned instances contain only
     * distinct Terms assigned to/occurring in a Resource together with information about how many times they occur and
     * whether they are suggested or asserted.
     *
     * @param resource Resource to get assignment info for
     * @return Aggregated assignment information for Resource
     */
    public List<ResourceTermAssignments> getAssignmentInfo(Resource resource) {
        return assignmentService.getResourceAssignmentInfo(resource);
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
        return resourceDao.findRelated(resource);
    }

    /**
     * Annotates a resource with vocabulary terms.
     *
     * @param resource Resource to be annotated
     * @param iTerms   Terms to be used for annotation
     */
    @Transactional
    public void setTags(Resource resource, final Collection<URI> iTerms) {
        assignmentService.setOnResource(resource, iTerms);
    }

    @Override
    protected void preRemove(Resource instance) {
        LOG.trace("Removing term occurrences in resource {} which is about to be removed.", instance);
        termOccurrenceDao.removeAll(instance);
        assignmentService.removeAll(instance);
        removeFromParentDocumentIfFile(instance);
    }

    private void removeFromParentDocumentIfFile(Resource instance) {
        if (!(instance instanceof File)) {
            return;
        }
        final File file = (File) instance;
        final Document parent = file.getDocument();
        if (parent != null) {
            LOG.trace("Removing file {} from its parent document {}.", instance, parent);
            // Need to detach the parent because we may want to merge it into a vocabulary context,
            // which would cause issues because it was originally loaded from the default context
            resourceDao.detach(parent);
            parent.removeFile(file);
            if (parent.getVocabulary() != null) {
                resourceDao.update(parent, vocabularyService.getRequiredReference(parent.getVocabulary()));
            } else {
                resourceDao.update(parent);
            }
        }
    }

    /**
     * Generates a resource identifier based on the specified label.
     *
     * @param label Resource label
     * @return Resource identifier
     */
    public URI generateIdentifier(String label) {
        Objects.requireNonNull(label);
        return idResolver.generateIdentifier(ConfigParam.NAMESPACE_RESOURCE, label);
    }
}
