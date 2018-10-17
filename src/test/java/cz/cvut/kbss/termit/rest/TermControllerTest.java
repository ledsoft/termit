package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
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
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TermControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/vocabularies";

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
    void doesLabelExistChecksForTermLabelExistenceInVocabulary() throws Exception {
        final String label = "test term";
        final String vocabName = "metropolitan-plan";
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
        when(idResolverMock.resolveIdentifier(namespace, vocabName)).thenReturn(URI.create(namespace + vocabName));
        when(termServiceMock.existsInVocabulary(any(), any())).thenReturn(true);
        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + vocabName + "/terms/label").param("namespace", namespace).param("label", label))
                                           .andExpect(status().isOk()).andReturn();
        assertTrue(readValue(mvcResult, Boolean.class));
        verify(termServiceMock).existsInVocabulary(label, URI.create(namespace + vocabName));
    }

    @Test
    void getByIdResolvesTermFullIdentifierAndLoadsTermFromService() throws Exception {
        final String vocabName = "metropolitan-plan";
        final String termName = "locality";
        final String vocabularyUri = Vocabulary.ONTOLOGY_IRI_termit + "/" + vocabName;
        final String namespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + vocabName +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + termName);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, vocabName))
                .thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(namespace);
        when(idResolverMock.resolveIdentifier(namespace, termName)).thenReturn(termUri);
        final Term term = Generator.generateTerm();
        term.setUri(termUri);
        when(termServiceMock.find(termUri)).thenReturn(Optional.of(term));
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + vocabName + "/terms/" + termName))
                                           .andExpect(status().isOk()).andReturn();
        final Term result = readValue(mvcResult, Term.class);
        assertEquals(term, result);
    }

    @Test
    void getByIdReturnsNotFoundForUnknownTermId() throws Exception {
        final String namespace = Vocabulary.ONTOLOGY_IRI_termit;
        final String vocabName = "metropolitan-plan";
        final String termName = "locality";
        final String vocabularyUri = namespace + "/" + vocabName;
        final String termNamespace = vocabularyUri + Constants.TERM_NAMESPACE_SEPARATOR + "/";
        final URI termUri = URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/" + vocabName +
                Constants.TERM_NAMESPACE_SEPARATOR + "/" + termName);
        when(idResolverMock.resolveIdentifier(namespace, vocabName)).thenReturn(URI.create(vocabularyUri));
        when(idResolverMock.buildNamespace(eq(vocabularyUri), any())).thenReturn(termNamespace);
        when(idResolverMock.resolveIdentifier(termNamespace, termName)).thenReturn(termUri);
        mockMvc.perform(get(PATH + "/" + vocabName + "/terms/" + termName).param("namespace", namespace))
               .andExpect(status().isNotFound());
        verify(termServiceMock).find(termUri);
    }
}