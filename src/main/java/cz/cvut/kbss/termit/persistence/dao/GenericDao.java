package cz.cvut.kbss.termit.persistence.dao;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Base interface for data access objects.
 *
 * @param <T> Type managed by this DAO
 */
public interface GenericDao<T> {

    /**
     * Finds all instances of the class managed by this DAO.
     *
     * @return All known instances
     */
    List<T> findAll();

    /**
     * Finds entity instance with the specified identifier.
     *
     * @param id Identifier
     * @return Entity instance or {@code null} if no such instance exists
     */
    Optional<T> find(URI id);

    /**
     * Persists the specified entity.
     *
     * @param entity Entity to persist
     */
    void persist(T entity);

    /**
     * Persists the specified instances.
     *
     * @param entities Entities to persist
     */
    void persist(Collection<T> entities);

    /**
     * Updates the specified entity.
     *
     * @param entity Entity to update
     * @return The updated entity. Use it for further processing, as it could be a completely different instance
     */
    T update(T entity);

    /**
     * Removes the specified entity.
     *
     * @param entity Entity to remove
     */
    void remove(T entity);

    /**
     * Removes an entity with the specified id.
     *
     * @param id Entity identifier
     */
    void remove(URI id);

    /**
     * Checks whether an entity with the specified id exists (and has the type managed by this DAO).
     *
     * @param id Entity identifier
     * @return {@literal true} if entity exists, {@literal false} otherwise
     */
    boolean exists(URI id);
}
