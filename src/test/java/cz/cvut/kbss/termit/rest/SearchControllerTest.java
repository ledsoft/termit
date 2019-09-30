package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.service.business.SearchService;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/search";

    @Mock
    private SearchService searchServiceMock;

    @InjectMocks
    private SearchController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void fullTextSearchExecutesSearchOnService() throws Exception {
        final List<FullTextSearchResult> expected = Collections
                .singletonList(new FullTextSearchResult(Generator.generateUri(), "test", null, Vocabulary.s_c_term, "test", "test", 1.0));
        when(searchServiceMock.fullTextSearch(any())).thenReturn(expected);
        final String searchString = "test";
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/fts").param("searchString", searchString))
                                           .andExpect(status().isOk()).andReturn();
        final List<FullTextSearchResult> result = readValue(mvcResult, new TypeReference<List<FullTextSearchResult>>() {
        });
        assertEquals(expected.size(), result.size());
        assertEquals(expected.get(0).getUri(), result.get(0).getUri());
        assertEquals(expected.get(0).getLabel(), result.get(0).getLabel());
        assertEquals(expected.get(0).getTypes(), result.get(0).getTypes());
    }
}