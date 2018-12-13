package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.net.URI;
import java.util.*;

@Service
public class ResourceRepositoryService extends BaseRepositoryService<Resource> {

    private final ResourceDao resourceDao;
    private final TermAssignmentDao termAssignmentDao;
    private final TargetDao targetDao;
    private final TermDao termDao;

    @Autowired
    public ResourceRepositoryService(Validator validator, ResourceDao resourceDao, TermDao termDao,
                                     TermAssignmentDao termAssignmentDao, TargetDao targetDao) {
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
     * @param resource Resource to be annotated
     * @param iTerms   Terms to be used for annotation
     */
    @Transactional
    public void setTags(Resource resource, final Collection<URI> iTerms) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(iTerms);

        // get the whole-resource target
        final Target target = targetDao.findByWholeResource(resource).orElseGet(() -> {
            final Target target2 = new Target(resource);
            targetDao.persist(target2);
            return target2;
        });

        // remove obsolete existing term assignments and determine new assignments to add
        final List<TermAssignment> termAssignments = termAssignmentDao.findByTarget(target);
        final Collection<URI> toAdd = new HashSet<>(iTerms);
        final List<TermAssignment> toRemove = new ArrayList<>(termAssignments.size());
        for (TermAssignment existing : termAssignments) {
            if (!iTerms.contains(existing.getTerm().getUri())) {
                toRemove.add(existing);
            } else {
                toAdd.remove(existing.getTerm().getUri());
            }
        }
        toRemove.forEach(termAssignmentDao::remove);

        // create term assignments for each input term to the target
        toAdd.forEach(iTerm -> {
            final Term term = termDao.find(iTerm).orElseThrow(
                    () -> NotFoundException.create(Term.class.getSimpleName(), iTerm));

            final TermAssignment termAssignment = new TermAssignment(term, target);
            termAssignmentDao.persist(termAssignment);
        });

        update(resource);
    }
}
