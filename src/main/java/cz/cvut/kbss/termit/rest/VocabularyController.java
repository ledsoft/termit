package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
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
public class VocabularyController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyRepositoryService vocabularyService;

    @Autowired
    public VocabularyController(VocabularyRepositoryService vocabularyService, IdentifierResolver idResolver,
                                Configuration config) {
        super(idResolver, config);
        this.vocabularyService = vocabularyService;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Vocabulary> getAll() {
        return vocabularyService.findAll();
    }

    @RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createVocabulary(@RequestBody Vocabulary vocabulary) {
        vocabularyService.persist(vocabulary);
        LOG.debug("Vocabulary {} created.", vocabulary);
        final HttpHeaders headers = generateLocationHeader(vocabulary.getUri(), ConfigParam.NAMESPACE_VOCABULARY);
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{fragment}", method = RequestMethod.GET)
    public Vocabulary getById(@PathVariable("fragment") String fragment,
                              @RequestParam(name = "namespace", required = false) String namespace) {
        final URI id = resolveVocabularyUri(fragment, namespace);
        return vocabularyService.find(id)
                                .orElseThrow(() -> NotFoundException.create(Vocabulary.class.getSimpleName(), id));
    }

    private URI resolveVocabularyUri(String fragment, String namespace) {
        final URI id;
        if (namespace != null) {
            id = idResolver.resolveIdentifier(namespace, fragment);
        } else {
            id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment);
        }
        return id;
    }

    @RequestMapping(value = "/{fragment}", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateVocabulary(@PathVariable("fragment") String fragment,
                                 @RequestParam(name = "namespace", required = false) String namespace,
                                 @RequestBody Vocabulary update) {
        final URI vocabularyUri = resolveVocabularyUri(fragment, namespace);
        if (!vocabularyUri.equals(update.getUri())) {
            throw new ValidationException(
                    "Resolved vocabulary id " + vocabularyUri + " does not match the id of the specified vocabulary.");
        }
        if (!vocabularyService.exists(vocabularyUri)) {
            throw NotFoundException.create(Vocabulary.class.getSimpleName(), vocabularyUri);
        }
        vocabularyService.update(update);
        LOG.debug("Vocabulary {} updated.");
    }

    /**
     * Returns identifier which would be generated by the application for the specified vocabulary name (using the
     * configured namespace).
     *
     * @param name Vocabulary name
     * @return Generated vocabulary identifier
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/identifier", method = RequestMethod.GET)
    public String generateIdentifier(@RequestParam("name") String name) {
        return idResolver.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, name).toString();
    }
}
