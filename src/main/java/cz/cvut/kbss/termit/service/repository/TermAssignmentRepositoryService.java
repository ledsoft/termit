package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.TargetDao;
import cz.cvut.kbss.termit.persistence.dao.TermAssignmentDao;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.util.Vocabulary;
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

        mergeAssignments(resource, termUris, true, false);

        LOG.trace("Finished setting tags on resource {}.", resource);
    }

    private void mergeAssignments(Resource resource, Collection<URI> termUris, boolean removeObsolete,
                                  boolean suggested) {
        if (termUris.isEmpty()) {
            return;
        }
        // get the whole-resource target
        final Target target = targetForResource(resource);

        // remove obsolete existing term assignments and determine new assignments to add
        final List<TermAssignment> termAssignments = termAssignmentDao.findByTarget(target);
        final Collection<URI> toAdd = new HashSet<>(termUris);
        final List<TermAssignment> toRemove = new ArrayList<>(termAssignments.size());
        for (TermAssignment existing : termAssignments) {
            if (!termUris.contains(existing.getTerm())) {
                toRemove.add(existing);
            } else {
                toAdd.remove(existing.getTerm());
            }
        }
        if (removeObsolete) {
            toRemove.forEach(termAssignmentDao::remove);
        }

        // create term assignments for each input term to the target
        createAssignments(target, toAdd, suggested);
    }

    private Target targetForResource(Resource resource) {
        return targetDao.findByWholeResource(resource).orElseGet(() -> {
            final Target target2 = new Target(resource);
            targetDao.persist(target2);
            return target2;
        });
    }

    private void createAssignments(Target target, Collection<URI> termUris, boolean suggested) {
        termUris.forEach(iTerm -> {
            if (!termDao.exists(iTerm)) {
                throw NotFoundException.create(Term.class.getSimpleName(), iTerm);
            }

            final TermAssignment termAssignment = new TermAssignment(iTerm, target);
            if (suggested) {
                termAssignment.addType(Vocabulary.s_c_navrzene_prirazeni_termu);
            }
            termAssignmentDao.persist(termAssignment);
        });
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
        Objects.requireNonNull(resource);
        Objects.requireNonNull(termUris);
        LOG.trace("Adding tags {} to resource {}.", termUris, resource);

        mergeAssignments(resource, termUris, false, false);

        LOG.trace("Finished adding tags to resource {}.", resource);
    }

    /**
     * Creates assignments for terms with the specified identifiers and adds them to the specified Resource as
     * "suggested".
     * <p>
     * Suggested terms may be treated differently by the application because they are usually created by an automated
     * service, without the user's intervention.
     * <p>
     * This method does not remove any existing assignments. It only adds new ones for Terms which are not yet assigned
     * to the Resource.
     *
     * @param resource Target Resource
     * @param termUris Identifiers of Terms to assign
     * @see #addToResource(Resource, Collection)
     */
    @Transactional
    public void addToResourceSuggested(Resource resource, Collection<URI> termUris) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(termUris);
        LOG.trace("Adding suggested tags {} to resource {}.", termUris, resource);

        mergeAssignments(resource, termUris, false, true);

        LOG.trace("Finished adding suggested tags to resource {}.", resource);
    }
}
