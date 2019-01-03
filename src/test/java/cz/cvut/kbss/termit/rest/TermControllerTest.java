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
import cz.cvut.kbss.termit.service.business.TermService;
import cz.cvut.kbss.termit.service.export.util.TypeAwareByteArrayResource;
import cz.cvut.kbss.termit.util.*;
import cz.cvut.kbss.termit.util.Constants.Excel;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.util.Constants.DEFAULT_PAGE_SPEC;
import static cz.cvut.kbss.termit.util.Constants.QueryParams.PAGE;
import static cz.cvut.kbss.termit.util.Constants.QueryParams.PAGE_SIZE;
import static cz.cvut.kbss.termit.util.Constants.TERM_NAMESPACE_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private TermService termServiceMock;

    @Mock
    private Configuration configMock;

    @InjectMocks
    private TermController sut;

    private cz.cvut.kbss.termit.model.Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.vocabulary = Generator.generateVocabulary();
        when(configMock.get(ConfigParam.NAMESPACE_VOCABULARY)).thenReturn(Vocabulary.ONTOLOGY_IRI_termit + "/");
        vocabulary.setName(VOCABULARY_NAME);
        vocabulary.setUri(URI.create(VOCABULARY_URI));
    }

    @Test
    void doesNameExistChecksForTermLabelExistenceInVocabulary() throws Exception {
        final String name = "test term";
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
        final URI vocabularyUri = URI.create(namespace + VOCABULARY_NAME);
        when(idResolverMock.resolveIdentifier(namespace, VOCABULARY_NAME)).thenReturn(vocabularyUri);
        when(termServiceMock.findVocabularyRequired(vocabularyUri)).thenReturn(vocabulary);
        when(termServiceMock.existsInVocabulary(any(), any())).thenReturn(true);
        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/name").param(QueryParams.NAMESPACE, namespace)
                                                                 .param("value", name))
                                           .andExpect(status().isOk()).andReturn();
        assertTrue(readValue(mvcResult, Boolean.class));
        verify(termServiceMock).existsInVocabulary(name, vocabulary);
    }

    @Test
    void getByIdResolvesTermFullIdentifierAndLoadsTermFromService() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.findRequired(termUri)).thenReturn(term);
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME))
                                           .andExpect(status().isOk()).andReturn();
        final Term result = readValue(mvcResult, Term.class);
        assertEquals(term, result);
    }

    @Test
    void updateUpdatesTerm() throws Exception {
        final URI termUri = initTermUriResolution();
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        mockMvc.perform(put(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).content(toJson(term)).contentType(
                MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isNoContent());
        verify(termServiceMock).update(term);
    }

    @Test
    void updateThrowsValidationExceptionWhenTermUriDoesNotMatchRequestPath() throws Exception {
        initTermUriResolution();
        final Term term = Generator.generateTermWithId();
        final MvcResult mvcResult = mockMvc
                .perform(put(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME).content(toJson(term)).contentType(
                        MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString("does not match the ID of the specified entity"));
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
        when(termServiceMock.findRequired(term.getUri())).thenReturn(term);
        final List<Term> children = IntStream.range(0, 3).mapToObj(i -> Generator.generateTermWithId()).collect(
                Collectors.toList());
        when(termServiceMock.findSubTerms(term)).thenReturn(children);

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
        when(termServiceMock.findRequired(termUri)).thenReturn(term);
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
        when(termServiceMock.findRequired(termUri)).thenReturn(term);
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
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termServiceMock.findAllRoots(eq(vocabulary), any(Pageable.class))).thenReturn(terms);

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + VOCABULARY_NAME + "/terms/")
                        .param(QueryParams.NAMESPACE, Vocabulary.ONTOLOGY_IRI_termit))
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
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findAllRoots(any(), anyString())).thenReturn(terms);
        final String searchString = "test";

        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms")
                .param(QueryParams.NAMESPACE, Vocabulary.ONTOLOGY_IRI_termit)
                .param("searchString", searchString)).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(termServiceMock).findAllRoots(vocabulary, searchString);
    }

    @Test
    void getSubTermsFindsSubTermsOfTermWithSpecifiedId() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        final Term parent = Generator.generateTermWithId();
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI, parent.getLabel())).thenReturn(parent.getUri());
        when(termServiceMock.findRequired(parent.getUri())).thenReturn(parent);
        final List<Term> children =
                IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId()).collect(Collectors.toList());
        when(termServiceMock.findSubTerms(parent)).thenReturn(children);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/" + parent.getLabel() + "/subterms"))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(children.size(), result.size());
        assertTrue(children.containsAll(result));
        verify(termServiceMock).findRequired(parent.getUri());
        verify(termServiceMock).findSubTerms(parent);
    }

    @Test
    void getAssignmentsReturnsGetsTermAssignmentsFromService() throws Exception {
        final Term term = Generator.generateTermWithId();
        term.setLabel(TERM_NAME);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI);
        when(termServiceMock.findRequired(any())).thenReturn(term);
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
    void getAllExportsTermsToCsvWhenAcceptMediaTypeIsSetToCsv() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final TypeAwareByteArrayResource export = new TypeAwareByteArrayResource(content.getBytes(),
                CsvUtils.MEDIA_TYPE,
                CsvUtils.FILE_EXTENSION);
        when(termServiceMock.exportGlossary(vocabulary, CsvUtils.MEDIA_TYPE)).thenReturn(Optional.of(export));

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(CsvUtils.MEDIA_TYPE)).andExpect(
                status().isOk());
        verify(termServiceMock).exportGlossary(vocabulary, CsvUtils.MEDIA_TYPE);
    }

    @Test
    void getAllReturnsCsvAsAttachmentWhenAcceptMediaTypeIsCsv() throws Exception {
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final String content = String.join(",", Term.EXPORT_COLUMNS);
        final TypeAwareByteArrayResource export = new TypeAwareByteArrayResource(content.getBytes(),
                CsvUtils.MEDIA_TYPE,
                CsvUtils.FILE_EXTENSION);
        when(termServiceMock.exportGlossary(vocabulary, CsvUtils.MEDIA_TYPE)).thenReturn(Optional.of(export));

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
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final TypeAwareByteArrayResource export = prepareExcel();
        when(termServiceMock.exportGlossary(vocabulary, Excel.MEDIA_TYPE)).thenReturn(Optional.of(export));

        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms/").accept(Excel.MEDIA_TYPE)).andExpect(
                status().isOk());
        verify(termServiceMock).exportGlossary(vocabulary, Excel.MEDIA_TYPE);
    }

    private TypeAwareByteArrayResource prepareExcel() throws Exception {
        final XSSFWorkbook wb = new XSSFWorkbook();
        final XSSFSheet s = wb.createSheet("test");
        s.createRow(0).createCell(0).setCellValue("test");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        return new TypeAwareByteArrayResource(bos.toByteArray(), Excel.MEDIA_TYPE, Excel.FILE_EXTENSION);
    }

    @Test
    void getAllReturnsExcelAttachmentWhenAcceptMediaTypeIsExcel() throws Exception {
        initNamespaceAndIdentifierResolution();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final TypeAwareByteArrayResource export = prepareExcel();
        when(termServiceMock.exportGlossary(vocabulary, Excel.MEDIA_TYPE)).thenReturn(Optional.of(export));

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + VOCABULARY_NAME + "/terms").accept(Excel.MEDIA_TYPE)).andReturn();
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION),
                containsString("filename=\"" + VOCABULARY_NAME + Excel.FILE_EXTENSION + "\""));
    }

    private void initNamespaceAndIdentifierResolution() {
        when(idResolverMock.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
    }

    @Test
    void getAllLoadsRootsFromCorrectPage() throws Exception {
        initNamespaceAndIdentifierResolution();
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termServiceMock.findAllRoots(eq(vocabulary), any(Pageable.class))).thenReturn(terms);
        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms").param(PAGE, "5").param(PAGE_SIZE, "100"))
               .andExpect(status().isOk());

        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(termServiceMock).findAllRoots(eq(vocabulary), captor.capture());
        assertEquals(PageRequest.of(5, 100), captor.getValue());
    }

    @Test
    void getAllCreatesDefaultPageRequestWhenPagingInfoIsNotSpecified() throws Exception {
        initNamespaceAndIdentifierResolution();
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termServiceMock.findAllRoots(eq(vocabulary), any(Pageable.class))).thenReturn(terms);
        mockMvc.perform(get(PATH + "/" + VOCABULARY_NAME + "/terms")).andExpect(status().isOk());

        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(termServiceMock).findAllRoots(eq(vocabulary), captor.capture());
        assertEquals(DEFAULT_PAGE_SPEC, captor.getValue());
    }

    @Test
    void createRootTermPassesNewTermToService() throws Exception {
        initNamespaceAndIdentifierResolution();

        final Term newTerm = Generator.generateTermWithId();
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        mockMvc.perform(post(PATH + "/" + VOCABULARY_NAME + "/terms").content(toJson(newTerm))
                                                                     .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isCreated());
        verify(termServiceMock).persistRoot(newTerm, vocabulary);
    }

    @Test
    void createRootTermReturnsLocationHeaderWithTermLocation() throws Exception {
        initNamespaceAndIdentifierResolution();

        final Term newTerm = Generator.generateTerm();
        newTerm.setUri(URI.create(NAMESPACE + TERM_NAME));
        newTerm.setLabel(TERM_NAME);
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        final MvcResult mvcResult = mockMvc
                .perform(post(PATH + "/" + VOCABULARY_NAME + "/terms").content(toJson(newTerm))
                                                                      .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andReturn();
        verifyLocationEquals(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME, mvcResult);
    }

    @Test
    void createSubTermPassesNewTermToServiceTogetherWithParentTerm() throws Exception {
        initNamespaceAndIdentifierResolution();

        final Term parent = Generator.generateTerm();
        parent.setUri(URI.create(NAMESPACE + TERM_NAME));
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI + TERM_NAMESPACE_SEPARATOR);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI + Constants.TERM_NAMESPACE_SEPARATOR, TERM_NAME))
                .thenReturn(parent.getUri());
        when(termServiceMock.findRequired(parent.getUri())).thenReturn(parent);
        final Term newTerm = Generator.generateTermWithId();
        when(termServiceMock.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        mockMvc.perform(
                post(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/subterms").content(toJson(newTerm))
                                                                                        .contentType(
                                                                                                MediaType.APPLICATION_JSON))
               .andExpect(status().isCreated());
        verify(termServiceMock).persistChild(newTerm, parent);
    }

    @Test
    void createSubTermReturnsLocationHeaderWithTermLocation() throws Exception {
        initNamespaceAndIdentifierResolution();

        final Term parent = Generator.generateTerm();
        parent.setUri(URI.create(NAMESPACE + TERM_NAME));
        when(idResolverMock.buildNamespace(VOCABULARY_URI, Constants.TERM_NAMESPACE_SEPARATOR))
                .thenReturn(VOCABULARY_URI + TERM_NAMESPACE_SEPARATOR);
        when(idResolverMock.resolveIdentifier(VOCABULARY_URI + Constants.TERM_NAMESPACE_SEPARATOR, TERM_NAME))
                .thenReturn(parent.getUri());
        when(termServiceMock.findRequired(parent.getUri())).thenReturn(parent);
        final Term newTerm = Generator.generateTerm();
        final String name = "child-term";
        newTerm.setUri(URI.create(NAMESPACE + name));
        final MvcResult mvcResult = mockMvc.perform(
                post(PATH + "/" + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/subterms").content(toJson(newTerm))
                                                                                        .contentType(
                                                                                                MediaType.APPLICATION_JSON))
                                           .andExpect(status().isCreated()).andReturn();
        verifyLocationEquals(PATH + "/" + VOCABULARY_NAME + "/terms/" + name, mvcResult);
    }
}