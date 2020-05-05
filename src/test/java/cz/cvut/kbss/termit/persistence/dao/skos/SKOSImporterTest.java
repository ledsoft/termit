package cz.cvut.kbss.termit.persistence.dao.skos;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.exception.UnsupportedImportMediaTypeException;
import cz.cvut.kbss.termit.persistence.dao.BaseDaoTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class SKOSImporterTest extends BaseDaoTestRunner {

    private static final String VOCABULARY_IRI = "http://onto.fel.cvut.cz/ontologies/application/termit/slovník";
    private static final String GLOSSARY_IRI = "http://onto.fel.cvut.cz/ontologies/application/termit/glosář";

    @Autowired
    private EntityManager em;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Configuration config;

    private final ValueFactory vf = SimpleValueFactory.getInstance();

    @Test
    void importVocabularyImportsGlossaryFromSpecifiedStream() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            sut.importVocabulary(Constants.Turtle.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl"));
        });
        transactional(() -> {
            final Repository repo = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repo.getConnection()) {
                assertTrue(conn.hasStatement(vf.createIRI(Vocabulary.s_c_uzivatel_termitu), RDF.TYPE, SKOS.CONCEPT,
                        false));
                assertTrue(
                        conn.hasStatement(vf.createIRI(Vocabulary.s_c_omezeny_uzivatel_termitu), RDF.TYPE, SKOS.CONCEPT,
                                false));
                assertTrue(conn.hasStatement(vf.createIRI(Vocabulary.s_c_zablokovany_uzivatel_termitu), RDF.TYPE,
                        SKOS.CONCEPT, false));
            }
        });
    }

    @Test
    void importThrowsIllegalArgumentExceptionWhenNoStreamIsProvided() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            assertThrows(IllegalArgumentException.class, () -> sut.importVocabulary(Constants.Turtle.MEDIA_TYPE));
        });

    }

    @Test
    void importInsertsImportedDataIntoContextBasedOnOntologyIdentifier() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            sut.importVocabulary(Constants.Turtle.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl"));
        });
        transactional(() -> {
            final Repository repo = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repo.getConnection()) {
                final List<Resource> contexts = Iterations.asList(conn.getContextIDs());
                assertFalse(contexts.isEmpty());
                final Optional<Resource> ctx = contexts.stream().filter(r -> r.stringValue().contains(GLOSSARY_IRI))
                                                       .findFirst();
                assertTrue(ctx.isPresent());
                assertThat(ctx.get().stringValue(),
                        containsString(config.get(ConfigParam.WORKING_VOCABULARY_CONTEXT_EXTENSION)));
                final List<Statement> inAll = Iterations.asList(conn.getStatements(null, null, null, false));
                final List<Statement> inCtx = Iterations.asList(conn.getStatements(null, null, null, false, ctx.get()));
                assertEquals(inAll.size(), inCtx.size());
            }
        });
    }

    @Test
    void importResolvesVocabularyIriForContextWhenMultipleStreamsWithGlossaryAndVocabularyAreProvided() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            sut.importVocabulary(Constants.Turtle.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl"),
                    Environment.loadFile("data/test-vocabulary.ttl"));
        });
        transactional(() -> {
            final Repository repo = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repo.getConnection()) {
                final List<Resource> contexts = Iterations.asList(conn.getContextIDs());
                assertEquals(1, contexts.size());
                assertThat(contexts.get(0).stringValue(), containsString(VOCABULARY_IRI));
            }
        });
    }

    @Test
    void importThrowsIllegalArgumentExceptionWhenTargetContextCannotBeDeterminedFromSpecifiedData() {
        final String input = "@prefix termit: <http://onto.fel.cvut.cz/ontologies/application/termit/> .\n" +
                "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix termit-pojem: <http://onto.fel.cvut.cz/ontologies/application/termit/pojem/> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ." +
                "termit-pojem:zablokovaný-uživatel-termitu\n" +
                "        a       <http://www.w3.org/2004/02/skos/core#Concept> ;\n" +
                "        <http://www.w3.org/2004/02/skos/core#broader>\n" +
                "                termit-pojem:uživatel-termitu , <https://slovník.gov.cz/základní/pojem/typ-objektu> ;\n" +
                "        <http://www.w3.org/2004/02/skos/core#inScheme>\n" +
                "                termit:glosář ;\n" +
                "        <http://www.w3.org/2004/02/skos/core#prefLabel>\n" +
                "                \"Blocked TermIt user\"@en , \"Zablokovaný uživatel TermItu\"@cs .";
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> sut
                            .importVocabulary(Constants.Turtle.MEDIA_TYPE, new ByteArrayInputStream(input.getBytes())));
            assertThat(ex.getMessage(), containsString("storage context cannot be determined"));
        });
    }

    @Test
    void importThrowsUnsupportedImportMediaTypeExceptionForUnsupportedDataType() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            assertThrows(UnsupportedImportMediaTypeException.class, () -> sut
                    .importVocabulary(Constants.Excel.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl")));
        });
    }

    @Test
    void importReturnsVocabularyInstanceConstructedFromImportedData() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            final cz.cvut.kbss.termit.model.Vocabulary result = sut
                    .importVocabulary(Constants.Turtle.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl"),
                            Environment.loadFile("data/test-vocabulary.ttl"));
            assertNotNull(result);
            assertEquals(VOCABULARY_IRI, result.getUri().toString());
            assertEquals("Vocabulary of system TermIt - vocabulary", result.getLabel());
        });
    }

    @Test
    void importGeneratesRelationshipsBetweenTermsAndVocabularyBasedOnSKOSInScheme() {
        transactional(() -> {
            final SKOSImporter sut = context.getBean(SKOSImporter.class);
            sut.importVocabulary(Constants.Turtle.MEDIA_TYPE, Environment.loadFile("data/test-glossary.ttl"),
                    Environment.loadFile("data/test-vocabulary.ttl"));
        });
        transactional(() -> {
            try (final RepositoryConnection conn = em.unwrap(Repository.class).getConnection()) {
                final List<Resource> terms = Iterations.stream(conn.getStatements(null, RDF.TYPE, SKOS.CONCEPT))
                                                       .map(Statement::getSubject).collect(Collectors.toList());
                assertFalse(terms.isEmpty());
                terms.forEach(t -> assertTrue(conn.getStatements(t, vf.createIRI(Vocabulary.s_p_je_pojmem_ze_slovniku),
                        vf.createIRI(VOCABULARY_IRI)).hasNext()));
            }
        });
    }
}
