package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.util.ValidationResult;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
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

    private final Validator validator;

    protected BaseRepositoryService(Validator validator) {
        this.validator = validator;
    }

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
        return result.map(this::postLoad);
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
     * <p>
     * The default behavior is to validate the specified instance.
     *
     * @param instance The instance to be persisted, not {@code null}
     */
    protected void prePersist(@NonNull T instance) {
        validate(instance);
    }

    /**
     * Merges the specified updated instance into the repository.
     *
     * @param instance The instance to merge
     */
    @Transactional
    public T update(T instance) {
        Objects.requireNonNull(instance);
        preUpdate(instance);
        final T result = getPrimaryDao().update(instance);
        assert result != null;
        postUpdate(result);
        return result;
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #update(Object)}.
     * <p>
     * The default behavior is to validate the specified instance.
     *
     * @param instance The instance to be updated, not {@code null}
     */
    protected void preUpdate(@NonNull T instance) {
        validate(instance);
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #update(Object)}.
     *
     * @param instance The updated instance which will be returned by {@link #update(Object)}, not {@code null}
     */
    protected void postUpdate(@NonNull T instance) {
        // Do nothing
    }

    /**
     * Removes the specified instance from the repository.
     *
     * @param instance The instance to remove
     */
    @Transactional
    public void remove(T instance) {
        Objects.requireNonNull(instance);
        preRemove(instance);
        getPrimaryDao().remove(instance);
        postRemove(instance);
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #remove(Object)}.
     * <p>
     * The default behavior is a no-op.
     *
     * @param instance The instance to be removed, not {@code null}
     */
    protected void preRemove(@NonNull T instance) {
        // Do nothing
    }

    /**
     * Override this method to plug custom behavior into the transactional cycle of {@link #remove(Object)}.
     * <p>
     * The default behavior is a no-op.
     *
     * @param instance The removed instance, not {@code null}
     */
    protected void postRemove(@NonNull T instance) {
        // Do nothing
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

    /**
     * Validates the specified instance, using JSR 380.
     * <p>
     * This assumes that the type contains JSR 380 validation annotations.
     *
     * @param instance The instance to validate
     * @throws ValidationException In case the instance is not valid
     */
    protected void validate(T instance) {
        final ValidationResult<T> validationResult = ValidationResult.of(validator.validate(instance));
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }
    }
}
