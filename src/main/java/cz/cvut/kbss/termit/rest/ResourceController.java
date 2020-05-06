/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.dto.assignment.ResourceTermAssignments;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.TextAnalysisRecord;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.ResourceService;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static cz.cvut.kbss.termit.util.ConfigParam.NAMESPACE_RESOURCE;

@RestController
@RequestMapping("/resources")
public class ResourceController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceController.class);

    private final ResourceService resourceService;

    @Autowired
    public ResourceController(IdentifierResolver idResolver, Configuration config,
                              ResourceService resourceService) {
        super(idResolver, config);
        this.resourceService = resourceService;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<List<Resource>> getAll(ServletWebRequest webRequest) {
        if (webRequest.checkNotModified(resourceService.getLastModified())) {
            return null;
        }
        return ResponseEntity.ok().lastModified(resourceService.getLastModified()).body(resourceService.findAll());
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createResource(@RequestBody Resource resource) {
        resourceService.persist(resource);
        LOG.debug("Resource {} created.", resource);
        return ResponseEntity.created(generateLocation(resource.getUri(), NAMESPACE_RESOURCE)).build();
    }

    @GetMapping(value = "/{normalizedName}", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Resource getResource(@PathVariable String normalizedName,
                                @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        return resourceService.findRequired(identifier);
    }

    @PutMapping(value = "/{normalizedName}", consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateResource(@PathVariable String normalizedName,
                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                               @RequestBody Resource resource) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        verifyRequestAndEntityIdentifier(resource, identifier);
        resourceService.update(resource);
        LOG.debug("Resource {} updated.", resource);
    }

    @GetMapping(value = "/{normalizedName}/content")
    public ResponseEntity<org.springframework.core.io.Resource> getContent(
            @PathVariable String normalizedName,
            @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
            @RequestParam(name = "attachment", required = false) boolean asAttachment) {
        final Resource resource = getResource(normalizedName, namespace);
        try {
            final TypeAwareResource content = resourceService.getContent(resource);
            final ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                                                                     .contentLength(content.contentLength())
                                                                     .contentType(MediaType.parseMediaType(
                                                                             content.getMediaType()
                                                                                    .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)));
            if (asAttachment) {
                builder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + normalizedName + "\"");
            }
            return builder.body(content);
        } catch (IOException e) {
            throw new TermItException("Unable to load content of resource " + resource, e);
        }
    }

    @PutMapping(value = "/{normalizedName}/content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveContent(@PathVariable String normalizedName,
                            @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                            @RequestParam(name = "file") MultipartFile attachment) {
        final Resource resource = getResource(normalizedName, namespace);
        try {
            resourceService.saveContent(resource, attachment.getInputStream());
        } catch (IOException e) {
            throw new TermItException(
                    "Unable to read file (fileName=\"" + attachment.getOriginalFilename() + "\") content from request",
                    e);
        }
        LOG.debug("Content saved for resource {}.", resource);
    }

    @RequestMapping(value = "/{normalizedName}/content", method = RequestMethod.HEAD)
    public ResponseEntity<Void> hasContent(@PathVariable String normalizedName,
                                           @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        return resourceService.hasContent(getResource(normalizedName, namespace)) ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{normalizedName}/files", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<File> getFiles(@PathVariable String normalizedName,
                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        return resourceService.getFiles(resourceService.getRequiredReference(identifier));
    }

    @PostMapping(value = "/{normalizedName}/files", consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> addFileToDocument(@PathVariable String normalizedName,
                                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                                  @RequestBody File file) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        resourceService.addFileToDocument(resourceService.findRequired(identifier), file);
        LOG.debug("File {} successfully added to document {}.", file, identifier);
        return ResponseEntity.created(createFileLocation(file.getUri(), normalizedName)).build();
    }

    private URI createFileLocation(URI childUri, String parentIdFragment) {
        final String u = generateLocation(childUri, NAMESPACE_RESOURCE).toString();
        return URI.create(u.replace("/" + parentIdFragment + "/files", ""));
    }

    /**
     * Runs text analysis on the specified resource.
     *
     * @param normalizedName Normalized name used to identify the resource
     * @param namespace      Namespace used for resource identifier resolution. Optional, if not specified, the
     *                       configured namespace is used
     * @param vocabularies   Identifiers of vocabularies to be used as sources of Terms for the text analysis
     */
    @PutMapping(value = "/{normalizedName}/text-analysis")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void runTextAnalysis(@PathVariable String normalizedName,
                                @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                @RequestParam(name = "vocabulary", required = false, defaultValue = "") Set<URI> vocabularies) {
        final Resource resource = getResource(normalizedName, namespace);
        resourceService.runTextAnalysis(resource, vocabularies);
        LOG.debug("Text analysis finished for resource {}.", resource);
    }

    /**
     * Gets the latest text analysis record for the specified resource.
     *
     * @param normalizedName Normalized name used to identify the resource
     * @param namespace      Namespace used for resource identifier resolution. Optional, if not specified, the *
     *                       configured namespace is used
     * @return Text analysis record
     */
    @GetMapping(value = "/{normalizedName}/text-analysis/records/latest", produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public TextAnalysisRecord getLatestTextAnalysisRecord(@PathVariable String normalizedName,
                                                          @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final Resource resource = getResource(normalizedName, namespace);
        return resourceService.findLatestTextAnalysisRecord(resource);
    }

    /**
     * Gets Resources related to the specified one.
     * <p>
     * Related resources mean they have at least one common assigned term
     *
     * @param normalizedName Normalized Resource name
     * @param namespace      Namespace used for resource identifier resolution. Optional, if not specified, the
     *                       configured namespace is used
     * @return List of related resources
     */
    @GetMapping(value = "/{normalizedName}/related", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Resource> getRelatedResources(@PathVariable String normalizedName,
                                              @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        return resourceService.findRelated(resourceService.getRequiredReference(identifier));
    }

    @GetMapping(value = "/{normalizedName}/terms", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@PathVariable String normalizedName,
                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        return resourceService.findTags(resourceService.getRequiredReference(identifier));
    }

    @PutMapping(value = "/{normalizedName}/terms", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTerms(@PathVariable String normalizedName,
                         @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                         @RequestBody List<URI> termIds) {
        final Resource resource = getResource(normalizedName, namespace);
        resourceService.setTags(resource, termIds);
    }

    @GetMapping(value = "/{normalizedName}/assignments", produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<TermAssignment> getAssignments(@PathVariable String normalizedName,
                                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        final Resource resource = resourceService.getRequiredReference(identifier);
        return resourceService.findAssignments(resource);
    }

    @GetMapping(value = "/{normalizedName}/assignments/aggregated", produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<ResourceTermAssignments> getAssignmentInfo(@PathVariable String normalizedName,
                                                           @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        final Resource resource = resourceService.getRequiredReference(identifier);
        return resourceService.getAssignmentInfo(resource);
    }

    /**
     * Gets the change history of a vocabulary with the specified identification
     */
    @GetMapping(value = "/{fragment}/history", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<AbstractChangeRecord> getHistory(@PathVariable String fragment,
                                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final Resource resource = resourceService
                .getRequiredReference(resolveIdentifier(namespace, fragment, NAMESPACE_RESOURCE));
        return resourceService.getChanges(resource);
    }

    @DeleteMapping(value = "/{normalizedName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeResource(@PathVariable String normalizedName,
                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI identifier = resolveIdentifier(namespace, normalizedName, NAMESPACE_RESOURCE);
        final Resource toRemove = resourceService.getRequiredReference(identifier);
        resourceService.remove(identifier);
        LOG.debug("Resource {} removed.", toRemove);
    }

    /**
     * Returns identifier which would be generated by the application for the specified resource name (using the
     * configured namespace).
     *
     * @param label Resource label
     * @return Generated resource identifier
     */
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/identifier")
    public URI generateIdentifier(@RequestParam("name") String label) {
        return resourceService.generateIdentifier(label);
    }

    //
    // NKOD API
    //

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/resource/terms", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getTerms(@RequestParam(name = "iri") URI resourceId) {
        return resourceService.findTags(resourceService.getRequiredReference(resourceId));
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/resource/related", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Resource> getRelatedResources(@RequestParam(name = "iri") URI resourceId) {
        return resourceService.findRelated(resourceService.getRequiredReference(resourceId));
    }
}
