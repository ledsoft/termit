package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.SearchService;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController extends BaseController {

    private final SearchService searchService;

    @Autowired
    public SearchController(IdentifierResolver idResolver, Configuration config, SearchService searchService) {
        super(idResolver, config);
        this.searchService = searchService;
    }

    @RequestMapping(value = "/fts", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<FullTextSearchResult> fullTextSearch(@RequestParam(name = "searchString") String searchString) {
        return searchService.fullTextSearch(searchString);
    }
}
