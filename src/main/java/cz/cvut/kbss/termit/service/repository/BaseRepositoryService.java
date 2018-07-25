package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Base implementation of repository services.
 * <p>
 * It contains the basic transactional CRUD operations. Subclasses are expected to provide DAO of the correct type,
 * which is used by the CRUD methods implemented by this base class.
 * <p>
 * In order to minimize chances of messing up the transactional behavior, subclasses *should not* override the main CRUD
 * methods and instead should provide custom business logic by overriding the helper hooks such as {@link
 * #prePersist(Object)}.
 *
 * @param <T> Domain object type managed by this service
 */
public abstract class BaseRepositoryService<T> {

    /**
     * Gets primary DAO which is used to implement the CRUD methods in this service.
     *
     * @return Data access object
     */
    protected abstract GenericDao<T> getPrimaryDao();

    // Read methods are intentionally not transactional because, for example, when postLoad manipulates the resulting
    // entity in any way, transaction commit would attempt to insert the change into the repository, which is not desired

    /**
     * Loads all instances of the type managed by this service from the repository.
     *
     * @return List of all matching instances
     */
    public List<T> findAll() {
        final List<T> loaded = getPrimaryDao().findAll();
        return loaded.stream().map(this::postLoad).collect(Collectors.toList());
    }

    /**
     * Finds an object with the specified id and returns it.
     *
     * @param id Identifier of the object to load
     * @return {@link Optional} with the loaded object or an empty one
     */
    public Optional<T> find(URI id) {
        final Optional<T> result = getPrimaryDao().find(id);
        return result.isPresent() ? Optional.ofNullable(postLoad(result.get())) : result;
    }

    /**
     * Override this method to plug custom behavior into {@link #find(URI)} or {@link #findAll()}.
     *
     * @param instance The loaded instance, not {@code null}
     */
    protected T postLoad(@NonNull T instance) {
        // Do nothing
        return instance;
    }

    /**
     * Persists the specified instance into the repository.
     *
     * @param instance The instance to persist
     */
    @Transactional
    public void persist(@NonNull T instance) {
        Objects.requireNonNull(instance);
        prePersist(instance);
        getPrimaryDao().persist(instance);
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #persist(Object)}.
     *
     * @param instance The instance to be persisted, not {@code null}
     */
    protected void prePersist(@NonNull T instance) {
        // Do nothing, intended for overriding
    }

    /**
     * Merges the specified updated instance into the repository.
     *
     * @param instance The instance to merge
     */
    @Transactional
    public void update(T instance) {
        Objects.requireNonNull(instance);
        preUpdate(instance);
        getPrimaryDao().update(instance);
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #update(Object)}.
     *
     * @param instance The instance to be updated, not {@code null}
     */
    protected void preUpdate(@NonNull T instance) {
        // Do nothing
    }

    /**
     * Removes the specified instance from the repository.
     *
     * @param instance The instance to remove
     */
    @Transactional
    public void remove(T instance) {
        getPrimaryDao().remove(instance);
    }

    /**
     * Removes an instance with the specified identifier from the repository.
     *
     * @param id ID of the instance to remove
     */
    @Transactional
    public void remove(URI id) {
        getPrimaryDao().remove(id);
    }

    /**
     * Checks whether an instance with the specified identifier exists in the repository.
     *
     * @param id ID to check
     * @return {@code true} if the instance exists, {@code false} otherwise
     */
    public boolean exists(URI id) {
        return getPrimaryDao().exists(id);
    }
}
