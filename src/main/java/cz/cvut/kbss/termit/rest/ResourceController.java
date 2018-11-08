package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final ResourceRepositoryService resourceService;

    private final SecurityUtils securityUtils;

    @Autowired
    public ResourceController(ResourceRepositoryService resourceService, SecurityUtils securityUtils) {
        this.resourceService = resourceService;
        this.securityUtils = securityUtils;
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/terms", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@RequestParam(name = "iri") URI resourceId) {
        return resourceService.findTerms(getResource(resourceId));
    }

    private Resource getResource(URI resourceId) {
        return resourceService.find(resourceId)
                              .orElseThrow(() -> NotFoundException.create(Resource.class.getSimpleName(), resourceId));
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/related", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<Resource> getRelatedResources(@RequestParam(name = "iri") URI resourceId) {
        final List<Resource> result = resourceService.findRelated(getResource(resourceId));
        // Clear author info for unauthenticated requests
        if (!securityUtils.isAuthenticated()) {
            result.forEach(r -> r.setAuthor(null));
        }
        return result;
    }
}
