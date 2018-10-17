package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.LanguageService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LanguageControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/language";

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private LanguageService serviceMock;

    @InjectMocks
    private LanguageController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void findAllReturnsAllExistingTypesForLanguage() throws Exception {
        final String vocabName = "metropolitan-plan";
        final String namespace = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
        when(idResolverMock.resolveIdentifier(namespace, vocabName)).thenReturn(URI.create(namespace + vocabName));
        final List<Term> types = new ArrayList<>();
        types.add(new Term());
        types.add(new Term());
        when(serviceMock.findAll("en")).thenReturn(types);
        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/types").param("language", "en"))
                                           .andExpect(status().isOk()).andReturn();
        assertEquals(types,readValue(mvcResult, new TypeReference<List<Term>>(){}));
    }
}