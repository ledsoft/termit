package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.ResourceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.List;

@Service
public class ResourceRepositoryService extends BaseRepositoryService<Resource> {

    private final ResourceDao resourceDao;

    @Autowired
    public ResourceRepositoryService(Validator validator, ResourceDao resourceDao) {
        super(validator);
        this.resourceDao = resourceDao;
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
}
