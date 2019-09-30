package cz.cvut.kbss.termit.service.business;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Declares Create, Retrieve, Update and Delete (CRUD) operations for business services.
 *
 * @param <T> Type of the concept managed by this service
 */
public interface CrudService<T> {

    /**
     * Gets all items of the type managed by this service from the repository.
     *
     * @return List of items
     */
    List<T> findAll();

    /**
     * Gets an item with the specified identifier.
     *
     * @param id Item identifier
     * @return Matching item wrapped in an {@code Optional}
     */
    Optional<T> find(URI id);

    /**
     * Gets an item with the specified identifier.
     *
     * @param id Item identifier
     * @return Matching item
     * @throws cz.cvut.kbss.termit.exception.NotFoundException When no matching item is found
     */
    T findRequired(URI id);

    /**
     * Gets a reference to an item with the specified identifier (with empty attribute values).
     *
     * @param id Item identifier
     * @return Matching item reference wrapped in an {@code Optional}
     */
    Optional<T> getReference(URI id);

    /**
     * Gets a reference to an item with the specified identifier (with empty attribute values).
     *
     * @param id Item identifier
     * @return Matching item reference
     * @throws cz.cvut.kbss.termit.exception.NotFoundException When no matching item is found
     */
    T getRequiredReference(URI id);

    /**
     * Checks if an item with the specified identifier exists.
     *
     * @param id Item identifier
     * @return Existence check result
     */
    boolean exists(URI id);

    /**
     * Persists the specified item.
     *
     * @param instance Item to save
     */
    void persist(T instance);

    /**
     * Updates the specified item.
     *
     * @param instance Item update data
     * @return The updated item
     */
    T update(T instance);
}
