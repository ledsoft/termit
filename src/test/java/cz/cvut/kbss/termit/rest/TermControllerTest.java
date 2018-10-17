package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}