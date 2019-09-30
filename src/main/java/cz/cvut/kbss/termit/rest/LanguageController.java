package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.language.LanguageService;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/language")
public class LanguageController extends BaseController {

    private final LanguageService service;

    @Autowired
    public LanguageController(
            IdentifierResolver idResolver,
            Configuration config,
            LanguageService service) {
        super(idResolver, config);
        this.service = service;
    }

    /**
     * @return List of types
     */
    @PreAuthorize("permitAll()")    // No need to secure this
    @RequestMapping(value = "/types", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAll(@RequestParam String language) {
        return service.getTypesForLang(language);
    }
}
