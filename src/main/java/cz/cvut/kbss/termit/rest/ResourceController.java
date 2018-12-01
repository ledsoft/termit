package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;

import java.util.Set;

import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/resources")
public class ResourceController extends BaseController {

    private final ResourceRepositoryService resourceService;

    private final SecurityUtils securityUtils;

    @Autowired
    public ResourceController(IdentifierResolver idResolver, Configuration config,
                              ResourceRepositoryService resourceService, SecurityUtils securityUtils) {
        super(idResolver, config);
        this.resourceService = resourceService;
        this.securityUtils = securityUtils;
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/terms", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
                                                                                       JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@RequestParam(name = "iri") URI resourceId) {
        return resourceService.findTerms(getResource(resourceId));
    }

    @RequestMapping(value = "/{normalizedName}", method = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Resource getResource(@PathVariable("normalizedName") String normalizedName,
                                @RequestParam(name = "namespace", required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, ConfigParam.NAMESPACE_RESOURCE);
        return getResource(identifier);
    }

    private Resource getResource(URI resourceId) {
        return resourceService.find(resourceId)
                              .orElseThrow(() -> NotFoundException.create(Resource.class.getSimpleName(), resourceId));
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/related", method = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE,
                                JsonLd.MEDIA_TYPE})
    public List<Resource> getRelatedResources(@RequestParam(name = "iri") URI resourceId) {
        final List<Resource> result = resourceService.findRelated(getResource(resourceId));
        // Clear author info for unauthenticated requests
        if (!securityUtils.isAuthenticated()) {
            result.forEach(r -> r.setAuthor(null));
        }
        return result;
    }

    @RequestMapping(value = "/resource/annotate", method = RequestMethod.POST,
                    consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTags(@RequestParam(name = "iri") URI resourceId,
                        @RequestParam(name = "tags") Set<URI> termIds) {
        resourceService.setTags(resourceId, termIds);
    }
}
