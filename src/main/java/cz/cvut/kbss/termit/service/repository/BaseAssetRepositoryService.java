package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;

import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base repository service implementation for asset managing services.
 *
 * @param <T> Asset type
 */
public abstract class BaseAssetRepositoryService<T extends Asset> extends BaseRepositoryService<T> {

    protected BaseAssetRepositoryService(Validator validator) {
        super(validator);
    }

    @Override
    protected abstract AssetDao<T> getPrimaryDao();

    /**
     * Gets the specified number of recently added/edited assets.
     * <p>
     * The returned assets are sorted by added/edited date in descending order.
     *
     * @param count Maximum number of assets returned
     * @return List of most recently added/edited assets
     */
    public List<T> findRecentlyEdited(int count) {
        final List<T> result = getPrimaryDao().findRecentlyEdited(count);
        return result.stream().map(this::postLoad).collect(Collectors.toList());
    }
}
