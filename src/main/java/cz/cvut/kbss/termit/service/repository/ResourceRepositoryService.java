package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.ResourceDao;
import cz.cvut.kbss.termit.persistence.dao.TargetDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import java.net.URI;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceRepositoryService extends BaseRepositoryService<Resource> {

    private final ResourceDao resourceDao;
    private final TermAssignmentDao termAssignmentDao;
    private final TargetDao targetDao;
    private final TermDao termDao;

    @Autowired
    public ResourceRepositoryService(Validator validator, ResourceDao resourceDao, TermDao termDao, TermAssignmentDao termAssignmentDao, TargetDao targetDao) {
        super(validator);
        this.resourceDao = resourceDao;
        this.termDao = termDao;
        this.termAssignmentDao = termAssignmentDao;
        this.targetDao = targetDao;
    }

    @Override
    protected GenericDao<Resource> getPrimaryDao() {
        return resourceDao;
    }

    /**
     * Gets terms the specified resource is annotated with.
     *
     * @param resource Annotated resource
     * @return List of terms annotating the specified resource
     */
    public List<Term> findTerms(Resource resource) {
        return resourceDao.findTerms(resource);
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
     * @param iResource Resource to be annotated.
     * @param iTerms Terms to be used for annotation
     */
    @Transactional
    public void setTags(final URI iResource, final Collection<URI> iTerms) {
        final Resource resource =
            resourceDao.find(iResource).orElseThrow(() -> NotFoundException
                .create(Resource.class.getSimpleName(), iResource));

        // get the whole-resource target
        final Target target = targetDao.findByWholeResource(resource).orElseGet(() -> {
            final Target target2 = new Target();
            target2.setSource(resource);
            targetDao.persist(target2);
            return target2;
        });

        // remove existing term assignments
        // TODO do not remove tags which will be set anyway
        System.out.println("REMOVING ");
        final List<TermAssignment> termAssignments = termAssignmentDao.findByTarget(target);
        termAssignments.stream().forEach(termAssignmentDao::remove);

        // create term assignments for each input term to the target
        iTerms.forEach( iTerm -> {
            final Term term = termDao.find(iTerm).orElseThrow(
                () -> NotFoundException.create(Term.class.getSimpleName(), iTerm));

            final TermAssignment termAssignment = new TermAssignment();
            termAssignment.setTerm(term);
            termAssignment.setTarget(target);
            termAssignmentDao.update(termAssignment);
        });

        update(resource);
    }
}
