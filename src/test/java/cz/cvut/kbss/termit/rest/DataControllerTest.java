package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.dto.RdfsResource;
import cz.cvut.kbss.termit.persistence.dao.DataDao;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataControllerTest extends BaseControllerTestRunner {

    @Mock
    private DataDao dataDaoMock;

    @InjectMocks
    private DataController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void getPropertiesLoadsPropertiesFromDao() throws Exception {
        // TODO Replace with RDF.PROPERTY once JOPA 0.10.8 is out
        final RdfsResource property = new RdfsResource(URI.create(Vocabulary.s_p_ma_krestni_jmeno), "Name", null,
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>");
        when(dataDaoMock.findAllProperties()).thenReturn(Collections.singletonList(property));
        final MvcResult mvcResult = mockMvc.perform(get("/data/properties")).andExpect(status().isOk()).andReturn();
        final List<RdfsResource> result = readValue(mvcResult, new TypeReference<List<RdfsResource>>() {
        });
        assertEquals(Collections.singletonList(property), result);
    }
}