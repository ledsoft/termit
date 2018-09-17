package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.repository.QueryService;
import cz.cvut.kbss.termit.util.Configuration;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers
    .requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QueryServiceTest extends BaseServiceTestRunner {

    private static final String query = "CONSTRUCT { <http://xyz/x> <http://xyz/y> ?c} { SELECT (COUNT(*) AS ?c) {?s ?p ?o} }";
    private static final String result = "[{ \"@id\": \"http://xyz/x\", \"http://xyz/y\": [ { \"@type\": \"http://www.w3.org/2001/XMLSchema#integer\", \"@value\": \"2627\" } ] } ]";

    @Autowired
    private RestTemplate restTemplate;

    @Mock
    private Configuration config;

    private QueryService sut;

    private MockRestServiceServer mockServer;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());

        when(config.get(REPOSITORY_URL)).thenReturn("http://testserver.org/endpoint");

        this.sut = new QueryService(restTemplate, config);
    }

    @Test
    void analyzeDocumentInvokesTextAnalysisServiceWithDocumentContent()
        throws ParseException {
        mockServer.expect(requestToUriTemplate(config.get(REPOSITORY_URL) + "?query={query}", query))
                  .andExpect(method(HttpMethod.GET))
                  .andExpect(header("Accept","application/ld+json"))
                  .andRespond(withSuccess(result, MediaType.APPLICATION_JSON));

        final String res = sut.query(query);
        final JSONParser p = new JSONParser();
        final JSONArray ae = (JSONArray) p.parse(result);
        final JSONArray aa = (JSONArray) p.parse(res);
        Assertions.assertEquals(ae,aa);
        mockServer.verify();
    }
}