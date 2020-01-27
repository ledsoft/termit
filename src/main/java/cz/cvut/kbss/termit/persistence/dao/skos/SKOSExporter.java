package cz.cvut.kbss.termit.persistence.dao.skos;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.Utils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayOutputStream;
import java.io.File;

@Repository
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SKOSExporter {

    private static final String GLOSSARY_EXPORT_QUERY = "skos" + File.separator + "exportGlossary.rq";
    private static final String TERMS_EXPORT_QUERY = "skos" + File.separator + "exportGlossaryTerms.rq";

    private org.eclipse.rdf4j.repository.Repository repository;
    private ValueFactory vf;

    private final Model model = new LinkedHashModel();

    @Autowired
    public SKOSExporter(EntityManager em) {
        this.repository = em.unwrap(org.eclipse.rdf4j.repository.Repository.class);
        vf = repository.getValueFactory();
    }

    public void exportGlossaryInstance(Vocabulary vocabulary) {
        try (final RepositoryConnection conn = repository.getConnection()) {
            final GraphQuery gq = conn.prepareGraphQuery(Utils.loadQuery(GLOSSARY_EXPORT_QUERY));
            gq.setBinding("vocabulary", vf.createIRI(vocabulary.getUri().toString()));
            evaluateAndAddToModel(gq);
        }
    }

    private void evaluateAndAddToModel(GraphQuery gq) {
        try (GraphQueryResult gqResult = gq.evaluate()) {
            while (gqResult.hasNext()) {
                model.add(gqResult.next());
            }
        }
    }

    public void exportGlossaryTerms(Vocabulary vocabulary) {
        try (final RepositoryConnection conn = repository.getConnection()) {
            final GraphQuery gq = conn.prepareGraphQuery(Utils.loadQuery(TERMS_EXPORT_QUERY));
            gq.setBinding("vocabulary", vf.createIRI(vocabulary.getUri().toString()));
            evaluateAndAddToModel(gq);
        }
    }

    public byte[] exportAsTtl() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Rio.write(model, bos, RDFFormat.TURTLE);
        return bos.toByteArray();
    }
}
