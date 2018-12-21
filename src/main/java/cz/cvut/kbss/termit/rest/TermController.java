package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.TermService;
import cz.cvut.kbss.termit.service.export.util.TypeAwareResource;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Constants.Excel;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import cz.cvut.kbss.termit.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * Optionally, the terms can be filtered by the specified search string, so that only terms with label matching the
     * specified string are returned.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @param pageSize             Limit the number of elements in the returned page. Optional
     * @param pageNo               Number of the page to return. Optional
     * @param searchString         String to filter term labels by. Optional
     * @return List of terms of the specific vocabulary
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE,
                    JsonLd.MEDIA_TYPE,
                    CsvUtils.MEDIA_TYPE,
                    Excel.MEDIA_TYPE})
    public ResponseEntity getAll(@PathVariable String vocabularyIdFragment,
                                 @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                 @RequestParam(name = QueryParams.PAGE_SIZE, required = false) Integer pageSize,
                                 @RequestParam(name = QueryParams.PAGE, required = false) Integer pageNo,
                                 @RequestParam(name = "searchString", required = false) String searchString,
                                 @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptType) {
        URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        final Optional<ResponseEntity> export = exportTerms(vocabularyUri, vocabularyIdFragment, acceptType);
        if (export.isPresent()) {
            return export.get();
        }
        final Vocabulary vocabulary = getVocabulary(vocabularyUri);
        if (searchString != null && !searchString.isEmpty()) {
            return ResponseEntity.ok(termService.findAllRoots(vocabulary, searchString));
        }
        return ResponseEntity.ok(termService.findAllRoots(vocabulary, createPageRequest(pageSize, pageNo)));
    }

    private Optional<ResponseEntity> exportTerms(URI vocabularyUri, String vocabularyNormalizedName, String mediaType) {
        final Optional<TypeAwareResource> content = termService.exportGlossary(getVocabulary(vocabularyUri), mediaType);
        return content.map(r -> ResponseEntity.ok()
                                              .contentLength(r.contentLength())
                                              .contentType(MediaType.parseMediaType(mediaType))
                                              .header(HttpHeaders.CONTENT_DISPOSITION,
                                                      "attachment; filename=\"" + vocabularyNormalizedName +
                                                              r.getFileExtension() + "\"")
                                              .body(r));
    }

    private Vocabulary getVocabulary(URI vocabularyUri) {
        return termService.findVocabulary(vocabularyUri);
    }

    /**
     * Creates new term in the specified vocabulary, possibly under the specified parent term.
     *
     * @param vocabularyIdFragment Vocabulary name
     * @param namespace            Vocabulary namespace. Optional
     * @param parentTerm           Parent term of the new term. Optional
     * @param term                 Vocabulary term that will be created
     * @return Response with {@code Location} header.
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms", method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createTerm(@PathVariable String vocabularyIdFragment,
                                           @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace,
                                           @RequestParam(name = "parentTermUri", required = false) String parentTerm,
                                           @RequestBody Term term) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        if (parentTerm != null && !parentTerm.isEmpty()) {
            termService.persistChild(term, findTerm(URI.create(parentTerm)));
        } else {
            termService.persistRoot(term, getVocabulary(vocabularyUri));
        }

        LOG.debug("Term {} in vocabulary {} created.", term, vocabularyUri);
        return ResponseEntity.created(generateLocation(vocabularyUri, ConfigParam.NAMESPACE_VOCABULARY)).build();
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
        return findTerm(termUri);
    }

    private URI getTermUri(String vocabIdFragment, String termIdFragment, String namespace) {
        return idResolver.resolveIdentifier(idResolver
                .buildNamespace(getVocabularyUri(namespace, vocabIdFragment).toString(),
                        Constants.TERM_NAMESPACE_SEPARATOR), termIdFragment);
    }

    private Term findTerm(URI termUri) {
        return termService.find(termUri).orElseThrow(() -> NotFoundException.create("Term", termUri));
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
        // TODO Move existence check on update to service
        if (!termService.exists(termUri)) {
            throw NotFoundException.create(Term.class.getSimpleName(), termUri);
        }
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

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}/assignments", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<TermAssignment> getAssignments(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                               @PathVariable("termIdFragment") String termIdFragment,
                                               @RequestParam(name = QueryParams.NAMESPACE, required = false) String namespace) {
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
