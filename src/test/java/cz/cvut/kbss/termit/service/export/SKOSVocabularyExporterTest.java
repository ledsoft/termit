package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SKOSVocabularyExporterTest extends VocabularyExporterTestBase {

    @Autowired
    private SKOSVocabularyExporter sut;

    @Autowired
    private Configuration config;

    private ValueFactory vf = SimpleValueFactory.getInstance();

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Test
    void supportsReturnsTrueForTurtleMediaType() {
        assertTrue(sut.supports(Constants.Turtle.MEDIA_TYPE));
    }

    @Test
    void supportsReturnsFalseForNonRdfSerializationMediaType() {
        assertFalse(sut.supports(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    void exportVocabularyGlossaryExportsGlossaryInfo() throws IOException {
        final TypeAwareResource result = sut.exportVocabularyGlossary(vocabulary);
        final Model model = loadAsModel(result);
        assertThat(model, hasItem(vf
                .createStatement(glossaryIri(vocabulary), RDF.TYPE, SKOS.CONCEPT_SCHEME)));
        assertThat(model, hasItem(vf
                .createStatement(glossaryIri(vocabulary), RDF.TYPE, OWL.ONTOLOGY)));
        assertThat(model, hasItem(vf
                .createStatement(glossaryIri(vocabulary), RDFS.LABEL,
                        vf.createLiteral(vocabulary.getLabel(), config.get(ConfigParam.LANGUAGE)))));
    }

    private Model loadAsModel(TypeAwareResource result) throws IOException {
        final RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));
        parser.parse(result.getInputStream(), "");
        return model;
    }

    private IRI glossaryIri(Vocabulary vocabulary) {
        return vf.createIRI(vocabulary.getGlossary().getUri().toString());
    }

    @Test
    void exportVocabularyGlossaryExportsImportsOfOtherGlossariesAsOWLImports() throws IOException {
        final Vocabulary anotherVocabulary = Generator.generateVocabularyWithId();
        vocabulary.setImportedVocabularies(Collections.singleton(anotherVocabulary.getUri()));
        transactional(() -> {
            em.persist(anotherVocabulary, DescriptorFactory.vocabularyDescriptor(anotherVocabulary));
            em.merge(vocabulary, DescriptorFactory.vocabularyDescriptor(vocabulary));
        });

        final TypeAwareResource result = sut.exportVocabularyGlossary(vocabulary);
        final Model model = loadAsModel(result);
        assertThat(model,
                hasItem(vf.createStatement(glossaryIri(vocabulary), OWL.IMPORTS, glossaryIri(anotherVocabulary))));
    }
}
