package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/vocabularies")
public class TermController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(TermController.class);

    private final TermRepositoryService termService;

    @Autowired
    public TermController(IdentifierResolver idResolver, Configuration config, TermRepositoryService termService) {
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
     * @param limit                Limit the number of elements to return. Optional
     * @param offset               Offset the first returned result. Optional
     * @param searchString         String to filter term labels by. Optional
     * @return List of terms of the specific vocabulary
     */
    @RequestMapping(value = "/{vocabularyIdFragment}/terms", method = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAll(@PathVariable String vocabularyIdFragment,
                             @RequestParam(name = "namespace", required = false) String namespace,
                             @RequestParam(name = "limit", required = false) Integer limit,
                             @RequestParam(name = "offset", required = false) Integer offset,
                             @RequestParam(name = "searchString", required = false) String searchString) {
        URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        // TODO: Change the limit/offset strategy to regular paging API (pageNo, page size)
        if (limit == null) {
            limit = Integer.MAX_VALUE;
        }
        if (offset == null) {
            offset = 0;
        }
        if (searchString != null && !searchString.isEmpty()) {
            return termService.findAll(searchString, vocabularyUri);
        }
        return termService.findAll(vocabularyUri, limit, offset);
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
                                           @RequestParam(name = "namespace", required = false) String namespace,
                                           @RequestParam(name = "parentTermUri", required = false) String parentTerm,
                                           @RequestBody Term term) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        if (parentTerm != null && !parentTerm.isEmpty()) {
            termService.addTermToVocabulary(term, vocabularyUri, URI.create(parentTerm));
        } else {
            termService.addTermToVocabulary(term, vocabularyUri);
        }

        LOG.debug("Term {} in vocabulary {} created.", term, vocabularyUri);
        final HttpHeaders headers = generateLocationHeader(vocabularyUri, ConfigParam.NAMESPACE_VOCABULARY);
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
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
                        @RequestParam(name = "namespace", required = false) String namespace) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        return termService.find(termUri).orElseThrow(() -> NotFoundException.create("Term", termUri));
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
                       @RequestParam(name = "namespace", required = false) String namespace,
                       @RequestBody Term term) {
        final URI termUri = getTermUri(vocabularyIdFragment, termIdFragment, namespace);
        if (!termUri.equals(term.getUri())) {
            throw new ValidationException(
                    "Resolved term id " + termUri + " does not match the id of the specified term.");
        }
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
                                  @RequestParam(name = "namespace", required = false) String namespace,
                                  @RequestParam(name = "searchString", required = false) String searchString) {
        final Term parent = getById(vocabularyIdFragment, termIdFragment, namespace);
        if (searchString != null && !searchString.isEmpty()) {
            // NOTE: Consider adding a dedicated search method in case this late filter causes performance problems
            final List<Term> searchResult = termService
                    .findAll(searchString, getVocabularyUri(namespace, vocabularyIdFragment));
            return searchResult.stream().filter(t -> parent.getSubTerms().contains(t.getUri()))
                               .collect(Collectors.toList());
        }
        return parent.getSubTerms() == null ? Collections.emptyList() :
                parent.getSubTerms().stream()
                      .map(uri -> termService.find(uri).orElseThrow(() -> NotFoundException.create("Term", uri)))
                      .collect(Collectors.toList());
    }

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/{termIdFragment}/assignments", method = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<TermAssignment> getAssignments(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                               @PathVariable("termIdFragment") String termIdFragment,
                                               @RequestParam(name = "namespace", required = false) String namespace) {
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
                                     @RequestParam(name = "namespace", required = false) String namespace,
                                     @RequestParam("name") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return idResolver.generateIdentifier(
                idResolver.buildNamespace(vocabularyUri.toString(), Constants.TERM_NAMESPACE_SEPARATOR), name)
                         .toString();
    }

    @RequestMapping(value = "/{vocabularyIdFragment}/terms/name", method = RequestMethod.GET)
    public Boolean doesNameExist(@PathVariable("vocabularyIdFragment") String vocabularyIdFragment,
                                 @RequestParam(name = "namespace", required = false) String namespace,
                                 @RequestParam(name = "value") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, vocabularyIdFragment);
        return termService.existsInVocabulary(name, vocabularyUri);
    }
}
