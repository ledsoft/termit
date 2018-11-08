package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/terms", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@RequestParam(name = "iri") URI resourceId) {
        // TODO
        return Collections.emptyList();
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/related", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<Resource> getRelatedResources(@RequestParam(name = "iri") URI resourceId) {
        // TODO
        return Collections.emptyList();
    }
}
