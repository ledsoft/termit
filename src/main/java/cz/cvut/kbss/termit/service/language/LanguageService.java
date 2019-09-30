package cz.cvut.kbss.termit.service.language;

import cz.cvut.kbss.termit.model.Term;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

/**
 * A service that fetches parts of the UFO-compliant language for the use in TermIt.
 * <p>
 * In this class:
 * - lang = natural language tag, e.g. "cs", or "en"
 * - language = UFO language, e.g. OntoUML, or Basic Language
 */
public abstract class LanguageService {

    protected ClassPathResource resource;

    public LanguageService(ClassPathResource languageTtlUrl) {
        this.resource = languageTtlUrl;
    }

    /**
     * Gets all types for the given lang.
     *
     * @param lang
     * @return
     */
    public abstract List<Term> getTypesForLang(String lang);
}
