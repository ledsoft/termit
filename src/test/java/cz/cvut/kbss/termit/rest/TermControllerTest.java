package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.jsonldjava.utils.JsonUtils;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermAssignment;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.export.VocabularyExporter;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/vocabularies";
    private static final String VOCABULARY_NAME = "metropolitan-plan";
    private static final String TERM_NAME = "locality";

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private TermRepositoryService termServiceMock;

    @Mock
    private VocabularyRepositoryService vocabularyServiceMock;

    @Mock
    private VocabularyExporter exporterMock;

    @InjectMocks
    private TermController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void doesNameExistChecksForTermLabelExistenceInVocabulary() throws Exception {
        final String name = "test term";
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
        when(idResolverMock.resolveIdentifier(namespace, VOCABULARY_NAME))
                .thenReturn(URI.create(namespace + VOCABULARY_NAME));
        when(termServiceMock.existsInVocabulary(any(), any())).thenReturn(true);
        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/name").param("namespace", namespace).param("value", name))
                                           .andExpect(status().isOk()).andReturn();
        assertTrue(readValue(mvcResult, Boolean.class));
        verify(termServiceMock).existsInVocabulary(name, URI.create(namespace + VOCABULARY_NAME));
    }

    @Test
    void getByIdResolvesTermFullIdentifierAndLoadsTermFromService() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.find(termUri)).thenReturn(Optional.of(term));
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME))
                                           .andExpect(status().isOk()).andReturn();
        final Term result = readValue(mvcResult, Term.class);
        assertEquals(term, result);
    }

    @Test
    void getByIdReturnsNotFoundForUnknownTermId() throws Exception {
        final String namespace = Vocabulary.ONTOLOGY_IRI_termit;
        final String vocabularyUri = namespace + "/" + VOCABULARY_NAME;
        final String termNamespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(namespace, VOCABULARY_NAME)).thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(termNamespace);
        when(idResolverMock.resolveIdentifier(termNamespace, TERM_NAME)).thenReturn(termUri);
        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).param("namespace", namespace))
               .andExpect(status().isNotFound());
        verify(termServiceMock).find(termUri);
    }

    @Test
    void updateUpdatesTerm() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.exists(termUri)).thenReturn(true);
        mockMvc.perform(put(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).content(toJson(term)).contentType(
                MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNoContent());
        verify(termServiceMock).update(term);
    }

    @Test
    void updateThrowsNotFoundWhenTermWithUriDoesNotExist() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.exists(termUri)).thenReturn(false);
        mockMvc.perform(put(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).content(toJson(term)).contentType(
                MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNotFound());
        verify(termServiceMock).exists(termUri);
        verify(termServiceMock, never()).update(any());
    }

    @Test
    void updateThrowsValidationExceptionWhenTermUriDoesNotMatchRequestPath() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(Generator.generateUri());
        final MvcResult mvcResult = mockMvc
                .perform(put(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).content(toJson(term)).contentType(
                        MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString("does not match the id of the specified term"));
        verify(termServiceMock, never()).exists(termUri);
        verify(termServiceMock, never()).update(any());
    }

    private URI initTermUriResolution() {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        when(idResolverMock.resolveIdentifier(namespace, TERM_NAME)).thenReturn(termUri);
        return termUri;
    }

    @Test
    void getSubTermsLoadsSubTermsOfParentTerm() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        final List<Term> children = IntStream.range(0, 3).mapToObj(i -> {
            final Term t = Generator.generateTerm();
            t.setUri(Generator.generateUri());
            when(termServiceMock.find(t.getUri())).thenReturn(Optional.of(t));
            return t;
        }).collect(Collectors.toList());
        term.setSubTerms(children.stream().map(Term::getUri).collect(Collectors.toSet()));
        when(termServiceMock.find(termUri)).thenReturn(Optional.of(term));

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/subterms"))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(children.size(), result.size());
        assertTrue(children.containsAll(result));
    }

    @Test
    void getSubTermsReturnsEmptyListForTermWithoutSubTerms() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.find(termUri)).thenReturn(Optional.of(term));
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/subterms"))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void generateIdentifierGeneratesTermIdentifierDerivedFromVocabularyId() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        when(idResolverMock.generateIdentifier(namespace, TERM_NAME)).thenReturn(termUri);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/identifier").param("name", TERM_NAME))
                .andExpect(status().isOk()).andReturn();
        final String result = readValue(mvcResult, String.class);
        assertEquals(termUri.toString(), result);
    }

    @Test
    void getTermReturnsTermWithUnmappedProperties() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        final String customProperty = Vocabulary.s_p_has_dataset;
        final String value = "Test";
        term.setProperties(Collections.singletonMap(customProperty, Collections.singleton(value)));
        term.setUri(termUri);
        when(termServiceMock.find(termUri)).thenReturn(Optional.of(term));
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).accept(
                JsonLd.MEDIA_TYPE)).andReturn();
        final Map jsonObj = (Map) JsonUtils.fromString(mvcResult.getResponse().getContentAsString());
        assertTrue(jsonObj.containsKey(customProperty));
        assertEquals(value, jsonObj.get(customProperty));
    }

    @Test
    void getAllReturnsAllTermsFromVocabulary() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        when(idResolverMock.resolveIdentifier(Vocabulary.ONTOLOGY_IRI_termit, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findAll(eq(URI.create(vocabularyUri)), anyInt(), anyInt())).thenReturn(terms);

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/").param("namespace", Vocabulary.ONTOLOGY_IRI_termit))
                                           .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(termServiceMock).findAll(URI.create(vocabularyUri), Integer.MAX_VALUE, 0);
    }

    @Test
    void getAllUsesSearchStringToFindMatchingTerms() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        when(idResolverMock.resolveIdentifier(Vocabulary.ONTOLOGY_IRI_termit, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findAll(any(), eq(URI.create(vocabularyUri)))).thenReturn(terms);
        final String searchString = "test";

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/")
                        .param("namespace", Vocabulary.ONTOLOGY_IRI_termit)
                        .param("searchString", searchString)).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(termServiceMock).findAll(searchString, URI.create(vocabularyUri));
    }

    @Test
    void getSubTermsFindsSubTermsOfTermWithSpecifiedId() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(vocabularyUri, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(vocabularyUri);
        when(idResolverMock.resolveIdentifier(vocabularyUri, parent.getLabel())).thenReturn(parent.getUri());
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> {
            final Term child = Generator.generateTermWithId();
            parent.addSubTerm(child.getUri());
            when(termServiceMock.find(child.getUri())).thenReturn(Optional.of(child));
            return child;
        }).collect(Collectors.toList());
        when(termServiceMock.find(parent.getUri())).thenReturn(Optional.of(parent));

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms"))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
        verify(termServiceMock).find(parent.getUri());
        terms.forEach(t -> verify(termServiceMock).find(t.getUri()));
    }

    @Test
    void getSubTermsFindsSubTermsBySearchString() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(vocabularyUri, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(vocabularyUri);
        when(idResolverMock.resolveIdentifier(vocabularyUri, parent.getLabel())).thenReturn(parent.getUri());
        when(termServiceMock.find(parent.getUri())).thenReturn(Optional.of(parent));
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> {
            final Term child = Generator.generateTermWithId();
            parent.addSubTerm(child.getUri());
            return child;
        }).collect(Collectors.toList());
        final String searchString = "test";
        final List<Term> searchResults = new ArrayList<>(terms);
        searchResults.add(Generator.generateTermWithId());
        when(termServiceMock.findAll(searchString, URI.create(vocabularyUri))).thenReturn(searchResults);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms")
                        .param("searchString", searchString)).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
        verify(termServiceMock).find(parent.getUri());
        verify(termServiceMock).findAll(searchString, URI.create(vocabularyUri));
    }

    @Test
    void getSubTermsThrowsNotFoundExceptionForUnknownTermIdentifier() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(vocabularyUri, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(vocabularyUri);
        when(idResolverMock.resolveIdentifier(vocabularyUri, parent.getLabel())).thenReturn(parent.getUri());
        when(termServiceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms"))
               .andExpect(status().isNotFound());
        verify(termServiceMock, never()).findAll(any(), any());
    }

    @Test
    void getAssignmentsReturnsGetsTermAssignmentsFromService() throws Exception {
        final Term term = Generator.generateTermWithId();
        term.setLabel(TERM_NAME);
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(vocabularyUri, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(vocabularyUri);
        when(termServiceMock.find(any())).thenReturn(Optional.of(term));
        final TermAssignment ta = new TermAssignment();
        ta.setTerm(term);
        ta.setTarget(new Target(Generator.generateResourceWithId()));
        when(termServiceMock.getAssignments(term)).thenReturn(Collections.singletonList(ta));

        final MvcResult mvcResult =
                mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/assignments")).andExpect(
                        status().isOk()).andReturn();
        final List<TermAssignment> result = readValue(mvcResult, new TypeReference<List<TermAssignment>>() {
        });
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ta.getTarget().getSource(), result.get(0).getTarget().getSource());
    }

    @Test
    void getAssignmentsThrowsNotFoundForUnknownTerm() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(vocabularyUri, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(vocabularyUri);
        when(termServiceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/assignments")).andExpect(
                status().isNotFound());
        verify(termServiceMock, never()).getAssignments(any());
    }

    @Test
    void getAllExportsTermsToCsvWhenAcceptMediaTypeIsSetToCsv() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final Resource export = new ByteArrayResource(content.getBytes());
        when(exporterMock.exportVocabularyGlossary(vocabulary)).thenReturn(export);

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(CsvUtils.MEDIA_TYPE)).andExpect(
                status().isOk());
        verify(exporterMock).exportVocabularyGlossary(vocabulary);
    }

    @Test
    void getAllReturnsCsvAsAttachmentWhenAcceptMediaTypeIsCsv() throws Exception {
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(vocabularyUri));
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final Resource export = new ByteArrayResource(content.getBytes());
        when(exporterMock.exportVocabularyGlossary(vocabulary)).thenReturn(export);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(CsvUtils.MEDIA_TYPE)).andReturn();
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION),
                containsString("filename=\"" + VOCABULARY_NAME + ".csv\""));
        assertEquals(content, mvcResult.getResponse().getContentAsString());
    }
}