package cz.cvut.kbss.termit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class TextAnalysisServiceTest extends BaseServiceTestRunner {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private TextAnalysisService sut;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
    }
}