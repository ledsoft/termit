package cz.cvut.kbss.termit.service.business;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Base interface for business services concerning assets.
 *
 * @param <T> Type of the asset, e.g. Vocabulary, Term
 */
public interface AssetService<T> {

    /**
     * Gets all assets of the type managed by this service from the repository.
     *
     * @return List of assets
     */
    List<T> findAll();

    /**
     * Gets asset with the specified identifier.
     *
     * @param id Resource identifier
     * @return Matching asset wrapped in an {@code Optional}
     */
    Optional<T> find(URI id);

    /**
     * Gets asset with the specified identifier.
     *
     * @param id Resource identifier
     * @return Matching asset
     * @throws cz.cvut.kbss.termit.exception.NotFoundException When no matching asset is found
     */
    T findRequired(URI id);

    /**
     * Checks if an asset with the specified identifier exists.
     *
     * @param id Asset identifier
     * @return Existence check result
     */
    boolean exists(URI id);

    /**
     * Persists the specified asset.
     *
     * @param instance Asset to save
     */
    void persist(T instance);

    /**
     * Updates the specified asset.
     *
     * @param instance Asset update data
     * @return The updated asset
     */
    T update(T instance);
}
