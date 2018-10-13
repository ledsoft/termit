package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.model.Term;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service public class LanguageService {

    private final ClassPathResource resource;

    public LanguageService() {
        resource = new ClassPathResource("language.ttl");
    }

    // TODO test
    public List<Term> findAll(String lang) {
        try {
            Model m = ModelFactory.createOntologyModel();
            m.read(resource.getURL().toString(),"text/turtle");

            final List<Term> terms = new ArrayList<>();
            m.listSubjectsWithProperty(RDF.type,
                ResourceFactory.createResource(cz.cvut.kbss.termit.util.Vocabulary.s_c_term))
             .forEachRemaining(c -> {
                 final Term t = new Term();
                 t.setUri(URI.create(c.getURI()));
                 t.setLabel(c.getProperty(RDFS.label, lang).getObject().asLiteral().getString());

                 final Statement st = c.getProperty(RDFS.comment, lang);
                 if ( st != null ) {
                     t.setComment(st.getObject().asLiteral().getString());
                 }
                 t.setSubTerms(c.listProperties(SKOS.narrower)
                                .mapWith(s -> URI.create(s.getObject().asResource().getURI()))
                                .toSet());
                 terms.add(t);
             });
            return terms;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
