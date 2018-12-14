package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.rest.handler.RestExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static cz.cvut.kbss.termit.environment.Environment.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Common configuration for REST controller tests.
 */
public class BaseControllerTestRunner {

    ObjectMapper objectMapper;

    ObjectMapper jsonLdObjectMapper;

    MockMvc mockMvc;

    public void setUp(Object controller) {
        setupObjectMappers();
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new RestExceptionHandler())
                                      .setMessageConverters(createJsonLdMessageConverter(),
                                              createDefaultMessageConverter(), createStringEncodingMessageConverter(),
                                              createResourceMessageConverter())
                                      .setUseSuffixPatternMatch(false)
                                      .build();
    }

    void setupObjectMappers() {
        this.objectMapper = Environment.getObjectMapper();
        this.jsonLdObjectMapper = Environment.getJsonLdObjectMapper();
    }

    String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    String toJsonLd(Object object) throws Exception {
        return jsonLdObjectMapper.writeValueAsString(object);
    }

    <T> T readValue(MvcResult result, Class<T> targetType) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), targetType);
    }

    <T> T readValue(MvcResult result, TypeReference<T> targetType) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), targetType);
    }

    void verifyLocationEquals(String expectedPath, MvcResult result) {
        final String locationHeader = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(locationHeader);
        final String path = locationHeader.substring(0,
                locationHeader.indexOf('?') != -1 ? locationHeader.indexOf('?') : locationHeader.length());
        assertEquals("http://localhost" + expectedPath, path);
    }
}
