package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.rest.util.RestUtils;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/vocabularies")
public class VocabularyController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyRepositoryService vocabularyService;

    private final IdentifierResolver idResolver;

    @Autowired
    public VocabularyController(VocabularyRepositoryService vocabularyService, IdentifierResolver idResolver) {
        this.vocabularyService = vocabularyService;
        this.idResolver = idResolver;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Vocabulary> getAll() {
        return vocabularyService.findAll();
    }

    @RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createVocabulary(@RequestBody Vocabulary vocabulary) {
        vocabularyService.persist(vocabulary);
        LOG.debug("Vocabulary {} created.", vocabulary);
        final HttpHeaders headers = RestUtils
                .createLocationHeaderFromCurrentUriWithPath("/{fragment}",
                        idResolver.extractIdentifierFragment(vocabulary.getUri()));
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{fragment}", method = RequestMethod.GET)
    public Vocabulary getById(@PathVariable("fragment") String fragment,
                              @RequestParam(name = "namespace", required = false) String namespace) {
        final URI id;
        if (namespace != null) {
            id = idResolver.resolveIdentifier(namespace, fragment);
        } else {
            id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment);
        }
        return vocabularyService.find(id)
                                .orElseThrow(() -> NotFoundException.create(Vocabulary.class.getSimpleName(), id));
    }
}
