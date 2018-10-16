package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}