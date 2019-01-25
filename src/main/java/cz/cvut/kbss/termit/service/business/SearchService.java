package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.persistence.dao.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    private final SearchDao searchDao;

    @Autowired
    public SearchService(SearchDao searchDao) {
        this.searchDao = searchDao;
    }

    /**
     * Searches resources by label.
     * <p>
     * Returns resources whose label matches the specified string.
     *
     * @param searchString String to search by
     * @return General representation of matching resources
     */
    public List<FullTextSearchResult> searchByLabel(String searchString) {
        return searchDao.fullTextSearch(searchString);
    }
}
