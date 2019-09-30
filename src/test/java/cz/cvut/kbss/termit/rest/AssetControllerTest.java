package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.business.AssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssetControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/assets";

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void getLastEditedRetrievesLastEditedAssetsFromService() throws Exception {
        final List<Asset> assets = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                            .collect(Collectors.toList());
        when(assetService.findLastEdited(anyInt())).thenReturn(assets);
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/last-edited")).andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(assets, result);
        verify(assetService).findLastEdited(Integer.parseInt(AssetController.DEFAULT_LIMIT));
    }

    @Test
    void getLastEditedUsesQueryParameterToSpecifyMaximumNumberOfReturnedResults() throws Exception {
        when(assetService.findLastEdited(anyInt())).thenReturn(Collections.emptyList());
        final int limit = 10;
        mockMvc.perform(get(PATH + "/last-edited").param("limit", Integer.toString(limit))).andExpect(status().isOk());
        verify(assetService).findLastEdited(limit);
    }
}