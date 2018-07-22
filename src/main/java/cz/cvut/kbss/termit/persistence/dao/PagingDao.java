package cz.cvut.kbss.termit.persistence.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PagingDao<T> extends GenericDao<T> {

    /**
     * Finds all instances corresponding to the page specification.
     *
     * @param pageSpec Page specification
     * @return Entities on page
     */
    Page<T> findAll(Pageable pageSpec);
}
