package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.dto.RdfsResource;
import cz.cvut.kbss.termit.persistence.dao.DataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class DataRepositoryService {

    private final DataDao dataDao;

    @Autowired
    public DataRepositoryService(DataDao dataDao) {
        this.dataDao = dataDao;
    }

    /**
     * Gets all properties present in the system.
     *
     * @return List of properties, ordered by label
     */
    public List<RdfsResource> findAllProperties() {
        return dataDao.findAllProperties();
    }

    /**
     * Gets basic metadata about a resource with the specified identifier.
     *
     * @param id Resource identifier
     * @return Wrapped matching resource or an empty {@code Optional} if no such resource exists
     */
    public Optional<RdfsResource> find(URI id) {
        return dataDao.find(id);
    }

    /**
     * Persists the specified property.
     * <p>
     *
     * @param property The property to persist
     */
    @Transactional
    public void persistProperty(RdfsResource property) {
        dataDao.persist(property);
    }

    /**
     * Gets the label of a resource with the specified identifier.
     *
     * @param id Resource identifier
     * @return Matching resource identifier (if found)
     */
    public Optional<String> getLabel(URI id) {
        return dataDao.getLabel(id);
    }
}
