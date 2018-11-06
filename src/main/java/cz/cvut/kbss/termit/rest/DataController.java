package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.dto.RdfsResource;
import cz.cvut.kbss.termit.persistence.dao.DataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provides access to general data from repository.
 * <p>
 * Note that this endpoint is currently not secured.
 */
@RestController
@RequestMapping("/data")
public class DataController {

    private final DataDao dataDao;

    @Autowired
    public DataController(DataDao dataDao) {
        this.dataDao = dataDao;
    }

    @RequestMapping(value = "/properties", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<RdfsResource> getProperties() {
        return dataDao.findAllProperties();
    }
}
