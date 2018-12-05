package cz.cvut.kbss.termit.service.language;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A service that fetches parts of the UFO-compliant language for the use in TermIt.
 * <p>
 * In this class: - lang = natural language tag, e.g. "cs", or "en" - language = UFO language, e.g. OntoUML, or Basic
 * Language
 */
@Qualifier("jena")
@Service
public class LanguageServiceJena extends LanguageService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageServiceJena.class);

    @Autowired
    public LanguageServiceJena(ClassPathResource languageTtlUrl) {
        super(languageTtlUrl);
    }

    /**
     * Gets all types for the given lang.
     *
     * @param lang
     * @return
     */
    public List<Term> getTypesForLang(String lang) {
        try {
            final Model m = ModelFactory.createOntologyModel();
            m.read(resource.getURL().toString(), "text/turtle");

            final List<Term> terms = new ArrayList<>();
            m.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(Vocabulary.s_c_term))
             .forEachRemaining(c -> {
                 final Term t = new Term();
                 t.setUri(URI.create(c.getURI()));
                 if (c.getProperty(RDFS.label, lang) != null) {
                     t.setLabel(c.getProperty(RDFS.label, lang).getObject().asLiteral().getString());
                 } else {
                     t.setLabel(t.getUri().toString());
                 }

                 final Statement st = c.getProperty(RDFS.comment, lang);
                 if (st != null) {
                     t.setComment(st.getObject().asLiteral().getString());
                 }
                 t.setSubTerms(c.listProperties(SKOS.narrower)
                                .mapWith(s -> URI.create(s.getObject().asResource().getURI())).toSet());
                 terms.add(t);
             });
            return terms;
        } catch (Exception e) {
            LOG.error("Unable to retrieve types for language {}.", lang, e);
            return Collections.emptyList();
        }
    }
}
