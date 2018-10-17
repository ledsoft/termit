package cz.cvut.kbss.termit.service.language;

import cz.cvut.kbss.termit.model.Term;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

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
