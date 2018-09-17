package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
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
import java.util.ArrayList;
import java.util.List;

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
        List<Term> terms = termService.findAll(vocabularyUri, limit, offset);
        LOG.debug("Get all terms for vocabulary {}", vocabularyUri);
        return terms;
    }

    @RequestMapping(value = "/{fragment}/terms/id", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Term getByID(@PathVariable String fragment,
                        @RequestParam(name = "term_id") String termId,
                        @RequestParam(name = "namespace", required = false) String namespace) {

        URI termUri = URI.create(termId);
        return termService.find(termUri).orElseThrow(() -> NotFoundException.create("Term", termId));
    }

    @RequestMapping(value = "/{fragment}/terms/subterms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Term> getSubtermsByParentID(@PathVariable String fragment,
                                            @RequestParam(name = "parent_id") String parentID,
                                            @RequestParam(name = "namespace", required = false) String namespace) {

        URI termUri = URI.create(parentID);
        return new ArrayList<>(termService.find(termUri)
                .orElseThrow(() -> NotFoundException.create("Term", parentID)).getSubTerms());

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
                                @RequestParam(name = "parentTerm") String parentTerm,
                                @RequestParam(name = "limit", required = false) Integer limit,
                                @RequestParam(name = "offset", required = false) Integer offset,
                                @RequestParam(name = "label", required = false) String label) {

        final URI vocabularyUri = getVocabularyUri(namespace, fragment);

        if (parentTerm != null && !parentTerm.equals("")) {
            Term term = termService.find(URI.create(parentTerm))
                    .orElseThrow(() -> NotFoundException.create("Term", parentTerm));
            return new ArrayList<>(term.getSubTerms());
        }
        if (label != null && !label.equals("")){
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
    @RequestMapping(value = "/{fragment}/terms/create", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createTerm(@PathVariable String fragment,
                                           @RequestParam(name = "namespace", required = false) String namespace,
                                           @RequestParam(name = "parentTermUri") String parentTerm,
                                           @RequestBody Term term) {

        URI vocabularyUri = getVocabularyUri(namespace, fragment);
        URI parentTermUri = null;
        if (parentTerm != null && !parentTerm.equals("")){
            parentTermUri = URI.create(parentTerm);
        }
        this.termService.addTermToVocabulary(term, vocabularyUri, parentTermUri);
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
    public String generateIdentifier(@PathVariable String fragment,
                                     @RequestParam(name = "namespace", required = false) String namespace,
                                     @RequestParam("name") String name) {
        URI vocabularyUri = getVocabularyUri(namespace, fragment);
        return idResolver.generateIdentifier(vocabularyUri.toString() + "/pojem", name).toString();
    }
}
