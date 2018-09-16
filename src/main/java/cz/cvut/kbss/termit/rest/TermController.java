package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
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
import java.util.List;

@RestController
@RequestMapping("/vocabularies")
public class TermController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(TermController.class);

    private final TermRepositoryService termService;
    private final TermDao termDao;

    @Autowired
    public TermController(IdentifierResolver idResolver, Configuration config, TermRepositoryService termService, TermDao termDao) {
        super(idResolver, config);
        this.termService = termService;
        this.termDao = termDao;
    }


    private URI getVocabularyUri(String fragment, String namespace){
        final URI vocabularyUri;
        if (namespace != null) {
            vocabularyUri = idResolver.resolveIdentifier(namespace, fragment);
        } else {
            vocabularyUri = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment);
        }
        return vocabularyUri;
    }

    /**
     *
     * @param fragment Vocabulary name
     * @param namespace Vocabulary namespace
     * @return List of terms of the specific vocabulary
     */
    @RequestMapping(value = "/{fragment}/terms", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAll(@PathVariable String fragment,
                             @RequestParam(name = "namespace", required = false) String namespace,
                             @RequestParam(name = "limit", required = false) Integer limit,
                             @RequestParam(name = "offset", required = false) Integer offset
                             ) {
        URI vocabularyUri = getVocabularyUri(fragment, namespace);
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

        final URI vocabularyUri = getVocabularyUri(fragment, namespace);

        URI parentTermUri = idResolver.resolveIdentifier(parentTerm, "");
        if (limit == null) {
            limit = 100;
        }
        if (offset == null) {
            offset = 0;
        }
        if (label == null) {
            label = "";
        }

        if (offset >= limit) {
            throw new ArrayIndexOutOfBoundsException("Offset cannot be bigger than limit");
        }

        return termDao.find(label, vocabularyUri, parentTermUri, offset, limit);
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
                                           @RequestBody Term term) {

        final URI vocabularyUri = getVocabularyUri(fragment, namespace);
        this.termService.addTermToVocabulary(term, vocabularyUri);
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
        URI vocabularyUri = getVocabularyUri(fragment, namespace);
        //TODO verify vocabulary.getUri().toString() is correct
        return idResolver.generateIdentifier(vocabularyUri.toString() + "/pojem", name).toString();
    }
}
