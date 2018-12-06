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
import cz.cvut.kbss.termit.service.export.VocabularyExporters;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Constants.Excel;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.util.Constants.NAMESPACE_PARAM;
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
    private static final String VOCABULARY_URI = Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME;
    private static final String NAMESPACE = VOCABULARY_URI + Constants.TERM_NAMESPACE_SEPARATOR + "/";

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private TermRepositoryService termServiceMock;

    @Mock
    private VocabularyRepositoryService vocabularyServiceMock;

    @Mock
    private VocabularyExporters exportersMock;

    @InjectMocks
    private TermController sut;

    private cz.cvut.kbss.termit.model.Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setName(VOCABULARY_NAME);
        vocabulary.setUri(URI.create(VOCABULARY_URI));
    }

    @Test
    void doesNameExistChecksForTermLabelExistenceInVocabulary() throws Exception {
        final String name = "test term";
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
        when(idResolverMock.resolveIdentifier(namespace, VOCABULARY_NAME))
                .thenReturn(URI.create(namespace + VOCABULARY_NAME));
        when(termServiceMock.existsInVocabulary(any(), any())).thenReturn(true);
        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/name").param(NAMESPACE_PARAM, namespace)
                                                                 .param("value", name))
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
        final String vocabularyNamespace = Vocabulary.ONTOLOGY_IRI_termit;
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(vocabularyNamespace, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        when(idResolverMock.resolveIdentifier(NAMESPACE, TERM_NAME)).thenReturn(termUri);
        mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).param(NAMESPACE_PARAM, vocabularyNamespace))
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
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        when(idResolverMock.resolveIdentifier(NAMESPACE, TERM_NAME)).thenReturn(termUri);
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
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + VOCABULARY_NAME +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        when(idResolverMock.generateIdentifier(NAMESPACE, TERM_NAME)).thenReturn(termUri);

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
        when(idResolverMock.resolveIdentifier(Vocabulary.ONTOLOGY_IRI_termit, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(vocabularyServiceMock.find(URI.create(VOCABULARY_URI))).thenReturn(Optional.of(vocabulary));
        when(termServiceMock.findAllRoots(eq(vocabulary), any())).thenReturn(terms);

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/").param(NAMESPACE_PARAM, Vocabulary.ONTOLOGY_IRI_termit))
                                           .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        when(termServiceMock.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC)).thenReturn(terms);
    }

    @Test
    void getAllUsesSearchStringToFindMatchingTerms() throws Exception {
        when(idResolverMock.resolveIdentifier(Vocabulary.ONTOLOGY_IRI_termit, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findAllRoots(any(), eq(URI.create(VOCABULARY_URI)))).thenReturn(terms);
        final String searchString = "test";

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/")
                        .param(NAMESPACE_PARAM, Vocabulary.ONTOLOGY_IRI_termit)
                        .param("searchString", searchString)).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(termServiceMock).findAllRoots(searchString, URI.create(VOCABULARY_URI));
    }

    @Test
    void getSubTermsFindsSubTermsOfTermWithSpecifiedId() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI, parent.getLabel())).thenReturn(parent.getUri());
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
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI, parent.getLabel())).thenReturn(parent.getUri());
        when(termServiceMock.find(parent.getUri())).thenReturn(Optional.of(parent));
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> {
            final Term child = Generator.generateTermWithId();
            parent.addSubTerm(child.getUri());
            return child;
        }).collect(Collectors.toList());
        final String searchString = "test";
        final List<Term> searchResults = new ArrayList<>(terms);
        searchResults.add(Generator.generateTermWithId());
        when(termServiceMock.findAllRoots(searchString, URI.create(VOCABULARY_URI))).thenReturn(searchResults);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms")
                        .param("searchString", searchString)).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms.size(), result.size());
        assertTrue(terms.containsAll(result));
        verify(termServiceMock).find(parent.getUri());
        verify(termServiceMock).findAllRoots(searchString, URI.create(VOCABULARY_URI));
    }

    @Test
    void getSubTermsThrowsNotFoundExceptionForUnknownTermIdentifier() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI, parent.getLabel())).thenReturn(parent.getUri());
        when(termServiceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms"))
               .andExpect(status().isNotFound());
        verify(termServiceMock, never()).findAllRoots(anyString(), any());
    }

    @Test
    void getAssignmentsReturnsGetsTermAssignmentsFromService() throws Exception {
        final Term term = Generator.generateTermWithId();
        term.setLabel(TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
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
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(termServiceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/assignments")).andExpect(
                status().isNotFound());
        verify(termServiceMock, never()).getAssignments(any());
    }

    @Test
    void getAllExportsTermsToCsvWhenAcceptMediaTypeIsSetToCsv() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final Resource export = new ByteArrayResource(content.getBytes());
        when(exportersMock.exportVocabularyGlossaryToCsv(vocabulary)).thenReturn(export);

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(CsvUtils.MEDIA_TYPE)).andExpect(
                status().isOk());
        verify(exportersMock).exportVocabularyGlossaryToCsv(vocabulary);
    }

    @Test
    void getAllReturnsCsvAsAttachmentWhenAcceptMediaTypeIsCsv() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final Resource export = new ByteArrayResource(content.getBytes());
        when(exportersMock.exportVocabularyGlossaryToCsv(vocabulary)).thenReturn(export);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(CsvUtils.MEDIA_TYPE)).andReturn();
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION),
                containsString("filename=\"" + VOCABULARY_NAME + CsvUtils.FILE_EXTENSION + "\""));
        assertEquals(content, mvcResult.getResponse().getContentAsString());
    }

    @Test
    void getAllExportsTermsToExcelWhenAcceptMediaTypeIsExcel() throws Exception {
        initNamespaceAndIdentifierResolution();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final Resource export = prepareExcel();
        when(exportersMock.exportVocabularyGlossaryToExcel(vocabulary)).thenReturn(export);

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(Excel.MEDIA_TYPE)).andExpect(
                status().isOk());
        verify(exportersMock).exportVocabularyGlossaryToExcel(vocabulary);
    }

    private Resource prepareExcel() throws Exception {
        final XSSFWorkbook wb = new XSSFWorkbook();
        final XSSFSheet s = wb.createSheet("test");
        s.createRow(0).createCell(0).setCellValue("test");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        return new ByteArrayResource(bos.toByteArray());
    }

    @Test
    void getAllReturnsExcelAttachmentWhenAcceptMediaTypeIsExcel() throws Exception {
        initNamespaceAndIdentifierResolution();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(vocabularyServiceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final Resource export = prepareExcel();
        when(exportersMock.exportVocabularyGlossaryToExcel(vocabulary)).thenReturn(export);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(Excel.MEDIA_TYPE)).andReturn();
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION),
                containsString("filename=\"" + VOCABULARY_NAME + Excel.FILE_EXTENSION + "\""));
    }

    private void initNamespaceAndIdentifierResolution() {
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
    }
}