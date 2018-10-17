package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.Term;
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
        final URI vocabularyUri;
        if (namespace != null) {
            vocabularyUri = idResolver.resolveIdentifier(namespace, fragment);
        } else {
            vocabularyUri = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment);
        }
        return vocabularyUri;
    }

    /**
     * @param fragment  Vocabulary name
     * @param namespace Vocabulary namespace
     * @return List of terms of the specific vocabulary
     */
    @RequestMapping(value = "/{fragment}/terms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAll(@PathVariable String fragment,
                             @RequestParam(name = "namespace", required = false) String namespace,
                             @RequestParam(name = "limit", required = false) Integer limit,
                             @RequestParam(name = "offset", required = false) Integer offset) {
        URI vocabularyUri = getVocabularyUri(namespace, fragment);
        if (limit == null) {
            limit = 100;
        }
        if (offset == null) {
            offset = 0;
        }
        return termService.findAll(vocabularyUri, limit, offset);
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
                                  @RequestParam(name = "namespace", required = false) String namespace) {
        final Term parent = getById(vocabularyIdFragment, termIdFragment, namespace);
        return parent.getSubTerms() == null ? Collections.emptyList() :
               parent.getSubTerms().stream()
                     .map(uri -> termService.find(uri).orElseThrow(() -> NotFoundException.create("Term", uri)))
                     .collect(Collectors.toList());

    }

    /**
     * @param fragment   Vocabulary name
     * @param namespace  Vocabulary namespace
     * @param parentTerm URI of the parent term
     * @param limit      number of terms that should be returned
     * @param offset     number of terms that should be skipped
     * @param label      term label (partial string)
     * @return List of terms that match all conditions (limit, offset, label and parentTerm)
     */
    @RequestMapping(value = "/{fragment}/terms/find", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> findTerms(@PathVariable String fragment,
                                @RequestParam(name = "namespace", required = false) String namespace,
                                @RequestParam(name = "parentTerm", required = false) String parentTerm,
                                @RequestParam(name = "limit", required = false) Integer limit,
                                @RequestParam(name = "offset", required = false) Integer offset,
                                @RequestParam(name = "label", required = false) String label) {

        final URI vocabularyUri = getVocabularyUri(namespace, fragment);

        if (parentTerm != null && !parentTerm.isEmpty()) {
            return getSubTerms(fragment, parentTerm, namespace);
        }
        if (label != null && !label.isEmpty()) {
            return termService.findAll(label, vocabularyUri);
        }
        if (limit == null) {
            limit = 100;
        }
        if (offset == null) {
            offset = 0;
        }

        return termService.findAll(vocabularyUri, limit, offset);
    }

    /**
     * @param fragment  Vocabulary name
     * @param namespace Vocabulary namespace
     * @param term      Vocabulary term that will be created
     * @return HttpHeader
     */
    @RequestMapping(value = "/{fragment}/terms", method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createTerm(@PathVariable String fragment,
                                           @RequestParam(name = "namespace", required = false) String namespace,
                                           @RequestParam(name = "parentTermUri", required = false) String parentTerm,
                                           @RequestBody Term term) {
        final URI vocabularyUri = getVocabularyUri(namespace, fragment);
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
     * Returns identifier which would be generated by the application for the specified vocabulary name (using the
     * configured namespace).
     *
     * @param name      Term name
     * @param fragment  Vocabulary name
     * @param namespace Vocabulary namespace
     * @return Generated term identifier for specific vocabulary
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/{fragment}/terms/identifier", method = RequestMethod.GET)
    public String generateIdentifier(@PathVariable("fragment") String fragment,
                                     @RequestParam(name = "namespace", required = false) String namespace,
                                     @RequestParam("name") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, fragment);
        return idResolver.generateIdentifier(
                idResolver.buildNamespace(vocabularyUri.toString(), Constants.TERM_NAMESPACE_SEPARATOR), name)
                         .toString();
    }

    @RequestMapping(value = "/{fragment}/terms/name", method = RequestMethod.GET)
    public Boolean doesNameExist(@PathVariable("fragment") String normalizedVocabName,
                                 @RequestParam(name = "namespace", required = false) String namespace,
                                 @RequestParam(name = "value") String name) {
        final URI vocabularyUri = getVocabularyUri(namespace, normalizedVocabName);
        return termService.existsInVocabulary(name, vocabularyUri);
    }
}
