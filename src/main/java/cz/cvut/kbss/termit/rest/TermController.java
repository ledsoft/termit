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
import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.TermService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants.Excel;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import cz.cvut.kbss.termit.util.Constants.Turtle;
import cz.cvut.kbss.termit.util.CsvUtils;
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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class TermController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(TermController.class);

    private final TermService termService;

    @Autowired
    public TermController(IdentifierResolver idResolver, Configuration config, TermService termService) {
        super(idResolver, config);
        this.termService = termService;
    }

    private URI getVocabularyUri(String namespace, String fragment) {
        return resolveIdentifier(namespace, fragment, ConfigParam.NAMESPACE_VOCABULARY);
    }

    /**
     * Get all terms from vocabulary with the specified identification.
     * <p>
     * This method also allows to export the terms into CSV or Excel by using HTTP content type negotiation or filter
     * terms by a search string.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @param searchString         String to filter term labels by. Optional
     * @return List of terms of the specific vocabulary
     */
    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms",
            produces = {MediaType.APPLICATION_JSON_VALUE,
                    JsonLd.MEDIA_TYPE,
                    CsvUtils.MEDIA_TYPE,
                    Excel.MEDIA_TYPE,
                    Turtle.MEDIA_TYPE})
    public ResponseEntity<?> getAll(@PathVariable String vocabularyIdFragment,
                                    @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                    @RequestParam(name = "searchString", required = false) String searchString,
                                    @RequestParam(name = "includeImported", required = false) boolean includeImported,
                                    @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptType) {
        URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        if (searchString != null) {
            return ResponseEntity.ok(includeImported ?
                                     termService.findAllIncludingImported(searchString, getVocabulary(vocabularyUri)) :
                                     termService.findAll(searchString, getVocabulary(vocabularyUri)));
        }
        final Optional<ResponseEntity<?>> export = exportTerms(vocabularyUri, vocabularyIdFragment, acceptType);
        return export.orElse(ResponseEntity.ok(termService.findAll(getVocabulary(vocabularyUri))));
    }

    private Optional<ResponseEntity<?>> exportTerms(URI vocabularyUri, String vocabularyNormalizedName,
                                                    String mediaType) {
        final Optional<TypeAwareResource> content =
                termService.exportGlossary(getVocabulary(vocabularyUri), mediaType);
        return content.map(r -> {
            try {
                return ResponseEntity.ok()
                                     .contentLength(r.contentLength())
                                     .contentType(MediaType.parseMediaType(mediaType))
                                     .header(HttpHeaders.CONTENT_DISPOSITION,
                                             "attachment; filename=\"" + vocabularyNormalizedName +
                                                     r.getFileExtension().orElse("") + "\"")
                                     .body(r);
            } catch (IOException e) {
                throw new TermItException("Unable to export terms.", e);
            }
        });
    }

    private Vocabulary getVocabulary(URI vocabularyUri) {
        return termService.findVocabularyRequired(vocabularyUri);
    }

    /**
     * Get all root terms from vocabulary with the specified identification.
     * <p>
     * Optionally, the terms can be filtered by the specified search string, so that only roots with descendants with
     * label matching the specified string are returned.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @param pageSize             Limit the number of elements in the returned page. Optional
     * @param pageNo               Number of the page to return. Optional
     * @param includeImported      Whether a transitive closure of vocabulary imports should be used when getting the
     *                             root terms. Optional, defaults to {@code false}
     * @return List of root terms of the specific vocabulary
     */
    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/roots",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAllRoots(@PathVariable String vocabularyIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                  @RequestParam(name = QueryParams.PAGE_SIZE, required = false) Integer pageSize,
                                  @RequestParam(name = QueryParams.PAGE, required = false) Integer pageNo,
                                  @RequestParam(name = "includeImported", required = false) boolean includeImported) {
        final Vocabulary vocabulary = getVocabulary(getVocabularyUri(namespace, vocabularyIdFragment));
        return includeImported ?
               termService.findAllRootsIncludingImports(vocabulary, createPageRequest(pageSize, pageNo)) :
               termService.findAllRoots(vocabulary, createPageRequest(pageSize, pageNo));
    }

    /**
     * Creates a new root term in the specified vocabulary.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @param term                 Vocabulary term that will be created
     * @return Response with {@code Location} header.
     * @see #createSubTerm(String, String, String, Term)
     */
    @PostMapping(value = "/vocabularies/{vocabularyIdFragment}/terms",
            consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createRootTerm(@PathVariable String vocabularyIdFragment,
                                               @RequestParam(name = QueryParams.NAMESPACE, required = false)
                                                       String namespace,
                                               @RequestBody Term term) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        termService.persistRoot(term, getVocabulary(vocabularyUri));

        LOG.debug("Root term {} created in vocabulary {}.", term, vocabularyUri);
        return ResponseEntity.created(generateLocation(term.getUri(), ConfigParam.NAMESPACE_VOCABULARY)).build();
    }

    /**
     * Gets term by its identifier fragment and vocabulary in which it is.
     *
     * @param vocabularyIdFragment Vocabulary identifier fragment
     * @param termIdFragment       Term identifier fragment
     * @param namespace            Vocabulary identifier namespace. Optional
     * @return Matching term
     * @throws NotFoundException If term does not exist
     */
    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Term getById(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                        @PathVariable("termIdFragment") String termIdFragment,
                        @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        return termService.findRequired(termUri);
    }

    /**
     * Gets term by its identifier.
     * <p>
     * This is a convenience method for accessing a Term without using its Vocabulary.
     *
     * @see #getById(String, String, String)
     */
    @GetMapping(value = "/terms/{termIdFragment}", produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Term getById(@PathVariable("termIdFragment") String termIdFragment,
                        @RequestParam(name = QueryParams.NAMESPACE) String namespace) {
        final URI termUri = idResolver.resolveIdentifier(namespace, termIdFragment);
        return termService.findRequired(termUri);
    }

    @DeleteMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeById(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                           @PathVariable("termIdFragment") String termIdFragment,
                           @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        termService.remove(termService.getRequiredReference(termUri));
        LOG.debug("Term {} removed.", termUri);
    }

    @DeleteMapping(value = "/terms/{termIdFragment}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeById(@PathVariable("termIdFragment") String termIdFragment,
                           @RequestParam(name = QueryParams.NAMESPACE) String namespace) {
        final URI termUri = idResolver.resolveIdentifier(namespace, termIdFragment);
        termService.remove(termService.getRequiredReference(termUri));
        LOG.debug("Term {} removed.", termUri);
    }

    private URI getTermUri(String vocabIdFragment, String termIdFragment, String namespace) {
        return idResolver.resolveIdentifier(idResolver
                .buildNamespace(getVocabularyUri(namespace, vocabIdFragment).toString(),
                        config.get(ConfigParam.TERM_NAMESPACE_SEPARATOR)), termIdFragment);
    }

    /**
     * Updates the specified term.
     *
     * @param vocabularyIdFragment Vocabulary identifier fragment
     * @param termIdFragment       Term identifier fragment
     * @param namespace            Vocabulary identifier namespace. Optional
     * @param term                 The updated term
     * @throws NotFoundException If term does not exist
     */
    @PutMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                       @PathVariable("termIdFragment") String termIdFragment,
                       @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                       @RequestBody Term term) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        verifyRequestAndEntityIdentifier(term, termUri);
        termService.update(term);
        LOG.debug("Term {} updated.", term);
    }

    /**
     * Updates the specified term.
     * <p>
     * This is a convenience method for accessing a Term without using its Vocabulary.
     *
     * @see #update(String, String, String, Term)
     */
    @PutMapping(value = "/terms/{termIdFragment}", consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("termIdFragment") String termIdFragment,
                       @RequestParam(name = QueryParams.NAMESPACE) String namespace,
                       @RequestBody Term term) {
        final URI termUri = idResolver.resolveIdentifier(namespace, termIdFragment);
        verifyRequestAndEntityIdentifier(term, termUri);
        termService.update(term);
        LOG.debug("Term {} updated.", term);
    }

    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}/subterms",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getSubTerms(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                  @PathVariable("termIdFragment") String termIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final Term parent = getById(vocabularyIdFragment, termIdFragment, namespace);
        return termService.findSubTerms(parent);
    }

    /**
     * A convenience endpoint for getting subterms of a Term without using its Vocabulary.
     */
    @GetMapping(value = "/terms/{termIdFragment}/subterms",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getSubTerms(@PathVariable("termIdFragment") String termIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE) String namespace) {
        final Term parent = getById(termIdFragment, namespace);
        return termService.findSubTerms(parent);
    }

    /**
     * Creates a new term under the specified parent Term in the specified vocabulary.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param parentIdFragment     Parent term identifier fragment
     * @param namespace            Vocabulary namespace. Optional
     * @param newTerm              Vocabulary term that will be created
     * @return Response with {@code Location} header.
     * @see #createRootTerm(String, String, Term)
     */
    @PostMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}/subterms",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createSubTerm(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                              @PathVariable("termIdFragment") String parentIdFragment,
                                              @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                              @RequestBody Term newTerm) {
        final Term parent = getById(vocabularyIdFragment, parentIdFragment, namespace);
        termService.persistChild(newTerm, parent);
        LOG.debug("Child term {} of parent {} created.", newTerm, parent);
        return ResponseEntity.created(createSubTermLocation(newTerm.getUri(), parentIdFragment)).build();
    }

    private URI createSubTermLocation(URI childUri, String parentIdFragment) {
        final String u = generateLocation(childUri, ConfigParam.NAMESPACE_VOCABULARY).toString();
        return URI.create(u.replace("/" + parentIdFragment + "/subterms", ""));
    }

    /**
     * Creates a new term under the specified parent Term.
     *
     * @see #createSubTerm(String, String, String, Term)
     */
    @PostMapping(value = "/terms/{termIdFragment}/subterms",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createSubTerm(@PathVariable("termIdFragment") String parentIdFragment,
                                              @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                              @RequestBody Term newTerm) {
        final Term parent = getById(parentIdFragment, namespace);
        termService.persistChild(newTerm, parent);
        LOG.debug("Child term {} of parent {} created.", newTerm, parent);
        return ResponseEntity.created(createSubTermLocation(newTerm.getUri(), parentIdFragment)).build();
    }

    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}/assignments",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<TermAssignments> getAssignmentInfo(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                                   @PathVariable("termIdFragment") String termIdFragment,
                                                   @RequestParam(name = QueryParams.NAMESPACE, required = false)
                                                           String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        return termService.getAssignmentInfo(termService.getRequiredReference(termUri));
    }

    /**
     * Gets assignment info for the specified Term.
     * <p>
     * This is a convenience method to allow access without using the Term's parent Vocabulary.
     *
     * @see #getAssignmentInfo(String, String, String)
     */
    @GetMapping(value = "/terms/{termIdFragment}/assignments", produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public List<TermAssignments> getAssignmentInfo(@PathVariable("termIdFragment") String termIdFragment,
                                                   @RequestParam(name = QueryParams.NAMESPACE) String namespace) {
        final URI termUri = idResolver.resolveIdentifier(namespace, termIdFragment);
        return termService.getAssignmentInfo(termService.getRequiredReference(termUri));
    }

    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/{termIdFragment}/history",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<AbstractChangeRecord> getHistory(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                                 @PathVariable("termIdFragment") String termIdFragment,
                                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        return termService.getChanges(termService.getRequiredReference(termUri));
    }

    /**
     * Gets history of changes of the specified Term.
     * <p>
     * This is a convenience method to allow access without using the Term's parent Vocabulary.
     *
     * @see #getHistory(String, String, String)
     */
    @GetMapping(value = "/terms/{termIdFragment}/history",
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<AbstractChangeRecord> getHistory(@PathVariable("termIdFragment") String termIdFragment,
                                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI termUri = idResolver.resolveIdentifier(namespace, termIdFragment);
        return termService.getChanges(termService.getRequiredReference(termUri));
    }

    /**
     * Returns identifier which would be generated by the application for the specified vocabulary name (using the
     * configured namespace).
     *
     * @param name                 Term name
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace
     * @return Generated term identifier for specific vocabulary
     */
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/identifier")
    public URI generateIdentifier(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                  @RequestParam("name") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return termService.generateIdentifier(vocabularyUri, name);
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/vocabularies/{vocabularyIdFragment}/terms/name")
    public Boolean doesNameExist(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                 @RequestParam(name = "value") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return termService.existsInVocabulary(name, getVocabulary(vocabularyUri));
    }
}
