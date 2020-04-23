package cz.cvut.kbss.termit.persistence.dao.skos;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.exception.DataImportException;
import cz.cvut.kbss.termit.exception.UnsupportedImportMediaTypeException;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SKOSImporter {

    private static final String VOCABULARY_TYPE = "https://slovník.gov.cz/veřejný-sektor/pojem/slovník";

    private static final Logger LOG = LoggerFactory.getLogger(SKOSImporter.class);

    private final Configuration config;

    private final Repository repository;
    private final ValueFactory vf;

    private final Model model = new LinkedHashModel();

    private String contextIriDiscriminator;

    private String vocabularyIri;

    @Autowired
    public SKOSImporter(Configuration config, EntityManager em) {
        this.config = config;
        this.repository = em.unwrap(org.eclipse.rdf4j.repository.Repository.class);
        vf = repository.getValueFactory();
    }

    public Vocabulary importVocabulary(String mediaType, InputStream... inputStreams) {
        if (inputStreams.length == 0) {
            throw new IllegalArgumentException("No input provided for importing vocabulary.");
        }
        LOG.debug("Vocabulary import started.");
        parseDataFromStreams(mediaType, inputStreams);
        resolveVocabularyIri();
        LOG.trace("Vocabulary identifier resolved to {}.", vocabularyIri);
        addDataIntoRepository();
        return constructVocabularyInstance();
    }

    private void parseDataFromStreams(String mediaType, InputStream... inputStreams) {
        final RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mediaType).orElseThrow(
                () -> new UnsupportedImportMediaTypeException("Media type" + mediaType + "not supported."));
        final RDFParser p = Rio.createParser(rdfFormat);
        final StatementCollector collector = new StatementCollector(model);
        p.setRDFHandler(collector);
        for (InputStream is : inputStreams) {
            try {
                p.parse(is, "");
            } catch (IOException e) {
                throw new DataImportException("Unable to parse data for import.", e);
            }
        }
    }

    private void addDataIntoRepository() {
        try (final RepositoryConnection conn = repository.getConnection()) {
            conn.begin();
            final String targetContext = generateContextIri(vocabularyIri);
            LOG.debug("Importing vocabulary into context <{}>.", targetContext);
            conn.add(model, vf.createIRI(targetContext));
            conn.commit();
        }
    }

    private void resolveVocabularyIri() {
        final Model res = model.filter(null, RDF.TYPE, OWL.ONTOLOGY);
        if (res.size() == 1) {
            this.vocabularyIri = res.iterator().next().getSubject().stringValue();
            return;
        }
        final Model resVocabulary = model.filter(null, RDF.TYPE, vf.createIRI(VOCABULARY_TYPE));
        if (resVocabulary.size() == 1) {
            this.vocabularyIri = resVocabulary.iterator().next().getSubject().stringValue();
            return;
        }
        throw new IllegalArgumentException(
                "No vocabulary or ontology found in the provided data. This means target storage context cannot be determined.");
    }

    private String generateContextIri(String baseIri) {
        final String contextIri = baseIri + config.get(ConfigParam.WORKING_VOCABULARY_CONTEXT_EXTENSION);
        return contextIriDiscriminator != null ? contextIri + "#" + contextIriDiscriminator.hashCode() : contextIri;
    }

    private Vocabulary constructVocabularyInstance() {
        final Vocabulary instance = new Vocabulary();
        instance.setUri(URI.create(vocabularyIri));
        final Set<Statement> labels = model.filter(vf.createIRI(vocabularyIri), RDFS.LABEL, null);
        labels.stream().filter(s -> {
            assert s.getObject() instanceof Literal;
            return Objects.equals(config.get(ConfigParam.LANGUAGE),
                    ((Literal) s.getObject()).getLanguage().orElse(config.get(ConfigParam.LANGUAGE)));
        }).findAny().ifPresent(s -> instance.setLabel(s.getObject().stringValue()));
        return instance;
    }

    /**
     * Guesses media type from the specified file name. E.g., if the file ends with ".ttl", the result will be {@link
     * cz.cvut.kbss.termit.util.Constants.Turtle#MEDIA_TYPE}.
     *
     * @param fileName File name used to guess media type
     * @return Guessed media type
     * @throws UnsupportedImportMediaTypeException If the media type could not be determined
     */
    public static String guessMediaType(String fileName) {
        return Rio.getParserFormatForFileName(fileName)
                  .orElseThrow(() -> new UnsupportedImportMediaTypeException("Unsupported type of file " + fileName))
                  .getDefaultMIMEType();
    }

    /**
     * Sets value of the context identifier discriminator.
     * <p>
     * The discriminator can be used in cases when a vocabulary is imported multiple times, so that the context IRI
     * (which is determined by the vocabulary identifier) can be parameterized and collisions are avoided.
     * <p>
     * The value is hashed when generating the context identifier.
     *
     * @param contextIriDiscriminator Value of the discriminator (will be hashed)
     */
    public void setContextIriDiscriminator(String contextIriDiscriminator) {
        this.contextIriDiscriminator = contextIriDiscriminator;
    }
}
