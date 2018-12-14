package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static cz.cvut.kbss.termit.util.Constants.NAMESPACE_PARAM;

@RestController
@RequestMapping("/resources")
public class ResourceController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceController.class);

    private final ResourceRepositoryService resourceService;

    private final SecurityUtils securityUtils;

    @Autowired
    public ResourceController(IdentifierResolver idResolver, Configuration config,
                              ResourceRepositoryService resourceService, SecurityUtils securityUtils) {
        super(idResolver, config);
        this.resourceService = resourceService;
        this.securityUtils = securityUtils;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Resource> getAll() {
        return resourceService.findAll();
    }

    @RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createResource(@RequestBody Resource resource) {
        resourceService.persist(resource);
        LOG.debug("Resource {} created.", resource);
        return ResponseEntity.created(generateLocation(resource.getUri(), ConfigParam.NAMESPACE_RESOURCE)).build();
    }

    @RequestMapping(value = "/{normalizedName}", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Resource getResource(@PathVariable("normalizedName") String normalizedName,
                                @RequestParam(name = NAMESPACE_PARAM, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, ConfigParam.NAMESPACE_RESOURCE);
        return getResource(identifier);
    }

    @RequestMapping(value = "/{normalizedName}", method = RequestMethod.PUT,
            consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateResource(@PathVariable("normalizedName") String normalizedName,
                               @RequestParam(name = NAMESPACE_PARAM, required = false) String namespace,
                               @RequestBody Resource resource) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, ConfigParam.NAMESPACE_RESOURCE);
        verifyRequestAndEntityIdentifier(resource, identifier);
        if (!resourceService.exists(identifier)) {
            throw NotFoundException.create(Resource.class.getSimpleName(), identifier);
        }
        resourceService.update(resource);
        LOG.debug("Resource {} updated.", resource);
    }

    @RequestMapping(value = "/{normalizedName}/terms", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTerms(@PathVariable("normalizedName") String normalizedName,
                         @RequestParam(name = NAMESPACE_PARAM, required = false) String namespace,
                         @RequestBody List<URI> termIds) {
        final Resource resource = getResource(normalizedName, namespace);
        resourceService.setTags(resource, termIds);
    }

    private Resource getResource(URI resourceId) {
        return resourceService.find(resourceId)
                              .orElseThrow(() -> NotFoundException.create(Resource.class.getSimpleName(), resourceId));
    }

    @RequestMapping(value = "/{normalizedName}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeResource(@PathVariable("normalizedName") String normalizedName,
                               @RequestParam(name = NAMESPACE_PARAM, required = false) String namespace) {
        final Resource toRemove = getResource(normalizedName, namespace);
        resourceService.remove(toRemove);
        LOG.debug("Resource {} removed.", toRemove);
    }

    //
    // NKOD API
    //

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/resource/terms", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@RequestParam(name = "iri") URI resourceId) {
        return resourceService.findTerms(getResource(resourceId));
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
}
