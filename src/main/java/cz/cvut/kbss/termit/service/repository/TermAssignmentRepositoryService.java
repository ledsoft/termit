package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.TargetDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;

@Service
public class TermAssignmentRepositoryService {

    private static final Logger LOG = LoggerFactory.getLogger(TermAssignmentRepositoryService.class);

    private final TermAssignmentDao termAssignmentDao;

    private final TargetDao targetDao;

    private final TermDao termDao;

    @Autowired
    public TermAssignmentRepositoryService(TermAssignmentDao termAssignmentDao,
                                           TargetDao targetDao, TermDao termDao) {
        this.termAssignmentDao = termAssignmentDao;
        this.targetDao = targetDao;
        this.termDao = termDao;
    }

    /**
     * Gets term assignments of the specified Resource.
     *
     * @param resource Resource for which assignments will be loaded
     * @return List of retrieved assignments
     */
    public List<TermAssignment> findAll(Resource resource) {
        Objects.requireNonNull(resource);
        return termAssignmentDao.findAll(resource);
    }

    /**
     * Removes all assignments to the specified Resource.
     *
     * @param resource Resource for which assignments will be removed
     */
    @Transactional
    public void removeAll(Resource resource) {
        Objects.requireNonNull(resource);
        final Optional<Target> target = targetDao.findByWholeResource(resource);
        target.ifPresent(t -> {
            LOG.trace("Removing term assignments to resource {}.", resource);
            final List<TermAssignment> assignments = termAssignmentDao.findByTarget(t);
            assignments.forEach(termAssignmentDao::remove);
            targetDao.remove(t);
        });
    }

    /**
     * Creates assignments for terms with the specified identifiers and sets them on the specified Resource.
     * <p>
     * If there already exists assignments on the resource, those representing Terms not found in the specified set will
     * be removed. Those representing Terms in the specified set will remain and no new assignments will be added for
     * them.
     *
     * @param resource Target Resource
     * @param termUris Identifiers of Terms to assign
     */
    @Transactional
    public void setOnResource(Resource resource, Collection<URI> termUris) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(termUris);
        LOG.trace("Setting tags {} on resource {}.", termUris, resource);

        // get the whole-resource target
        final Target target = targetDao.findByWholeResource(resource).orElseGet(() -> {
            final Target target2 = new Target(resource);
            targetDao.persist(target2);
            return target2;
        });

        // remove obsolete existing term assignments and determine new assignments to add
        final List<TermAssignment> termAssignments = termAssignmentDao.findByTarget(target);
        final Collection<URI> toAdd = new HashSet<>(termUris);
        final List<TermAssignment> toRemove = new ArrayList<>(termAssignments.size());
        for (TermAssignment existing : termAssignments) {
            if (!termUris.contains(existing.getTerm().getUri())) {
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

        LOG.trace("Finished setting tags on resource {}.", resource);
    }

    /**
     * Creates assignments for terms with the specified identifiers and adds them to the specified Resource.
     * <p>
     * This method does not remove any existing assignments. It only adds new ones for Terms which are not yet assigned
     * to the Resource.
     *
     * @param resource Target Resource
     * @param termUris Identifiers of Terms to assign
     * @see #setOnResource(Resource, Collection)
     */
    @Transactional
    public void addToResource(Resource resource, Collection<URI> termUris) {

    }
}
