package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.TermService;
import cz.cvut.kbss.termit.util.*;
import cz.cvut.kbss.termit.util.Constants.Excel;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
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
@RequestMapping("/vocabularies")
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
     * This method also allows to export the terms into CSV or Excel by using HTTP content type negotiation.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @return List of terms of the specific vocabulary
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE,
                    JsonLd.MEDIA_TYPE,
                    CsvUtils.MEDIA_TYPE,
                    Excel.MEDIA_TYPE})
    public ResponseEntity getAll(@PathVariable String vocabularyIdFragment,
                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                 @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptType) {
        URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        final Optional<ResponseEntity> export = exportTerms(vocabularyUri, vocabularyIdFragment, acceptType);
        return export.orElse(ResponseEntity.ok(termService.findAll(getVocabulary(vocabularyUri))));
    }

    private Optional<ResponseEntity> exportTerms(URI vocabularyUri, String vocabularyNormalizedName, String mediaType) {
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
     * @param searchString         String to filter term labels by. Optional
     * @return List of root terms of the specific vocabulary
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms/roots", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAllRoots(@PathVariable String vocabularyIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                  @RequestParam(name = QueryParams.PAGE_SIZE, required = false) Integer pageSize,
                                  @RequestParam(name = QueryParams.PAGE, required = false) Integer pageNo,
                                  @RequestParam(name = "searchString", required = false) String searchString) {
        final Vocabulary vocabulary = getVocabulary(getVocabularyUri(namespace, vocabularyIdFragment));
        if (searchString != null && !searchString.trim().isEmpty()) {
            return termService.findAllRoots(vocabulary, searchString);
        }
        return termService.findAllRoots(vocabulary, createPageRequest(pageSize, pageNo));
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
    @RequestMapping(value = "/{vocabularyIdFragment}/terms", method = RequestMethod.POST,
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
    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public Term getById(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                        @PathVariable("termIdFragment") String termIdFragment,
                        @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        return termService.findRequired(termUri);
    }

    private URI getTermUri(String vocabIdFragment, String termIdFragment, String namespace) {
        return idResolver.resolveIdentifier(idResolver
                .buildNamespace(getVocabularyUri(namespace, vocabIdFragment).toString(),
                        Constants.TERM_NAMESPACE_SEPARATOR), termIdFragment);
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
    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}", method = RequestMethod.PUT,
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

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}/subterms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getSubTerms(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                  @PathVariable("termIdFragment") String termIdFragment,
                                  @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
        final Term parent = getById(vocabularyIdFragment, termIdFragment, namespace);
        return termService.findSubTerms(parent);
    }

    /**
     * Creates a new term under the specified parent term in the specified vocabulary.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param parentIdFragment     Parent term identifier fragment
     * @param namespace            Vocabulary namespace. Optional
     * @param newTerm              Vocabulary term that will be created
     * @return Response with {@code Location} header.
     * @see #createRootTerm(String, String, Term)
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}/subterms", method = RequestMethod.POST,
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

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}/assignments", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<TermAssignment> getAssignments(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                               @PathVariable("termIdFragment") String termIdFragment,
                                               @RequestParam(name = QueryParams.NAMESPACE, required = false)
                                                       String namespace) {
        final Term term = getById(vocabularyIdFragment, termIdFragment, namespace);
        return termService.getAssignments(term);
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
    @RequestMapping(value = "/{vocabularyIdFragment}/terms/identifier", method = RequestMethod.GET)
    public String generateIdentifier(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                     @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                     @RequestParam("name") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return idResolver.generateIdentifier(
                idResolver.buildNamespace(vocabularyUri.toString(), Constants.TERM_NAMESPACE_SEPARATOR), name)
                         .toString();
    }

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/name", method = RequestMethod.GET)
    public Boolean doesNameExist(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                 @RequestParam(name = "value") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return termService.existsInVocabulary(name, getVocabulary(vocabularyUri));
    }
}
