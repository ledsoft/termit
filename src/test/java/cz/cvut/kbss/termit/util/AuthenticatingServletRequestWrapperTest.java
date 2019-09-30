package cz.cvut.kbss.termit.util;

import cz.cvut.kbss.termit.util.AdjustedUriTemplateProxyServlet.AuthenticatingServletRequestWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticatingServletRequestWrapperTest {

    private static final String USERNAME = "testUsername";
    private static final String PASSWORD = "testPassword";

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        this.mockRequest = new MockHttpServletRequest();
    }

    @Test
    void getHeaderReturnsBasicAuthenticationForAuthorizationWhenUsernameIsConfigured() {
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, USERNAME,
                PASSWORD);
        final String authHeader = sut.getHeader(HttpHeaders.AUTHORIZATION);
        checkBasicAuthHeader(authHeader);
    }

    private void checkBasicAuthHeader(String authHeader) {
        assertFalse(authHeader.isEmpty());
        assertThat(authHeader, startsWith("Basic"));
        final String value = authHeader.substring("Basic ".length());
        final String result = new String(Base64.getDecoder().decode(value));
        assertEquals(USERNAME + ":" + PASSWORD, result);
    }

    @Test
    void getHeaderReturnsRequestHeaderForAuthorizationWhenUsernameIsNotConfigured() {
        final String authValue = "Bearer aaaaaa";
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, authValue);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, "", "");
        final String authHeader = sut.getHeader(HttpHeaders.AUTHORIZATION);
        assertFalse(authHeader.isEmpty());
        assertEquals(authValue, authHeader);
    }

    @Test
    void getHeaderReturnsRequestHeaderForNonAuthorization() {
        mockRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, USERNAME,
                PASSWORD);
        final String result = sut.getHeader(HttpHeaders.ACCEPT);
        assertFalse(result.isEmpty());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, result);
    }

    @Test
    void getHeadersReturnsBasicAuthenticationForAuthorizationWhenUsernameIsConfigured() {
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, USERNAME,
                PASSWORD);
        final Enumeration<String> authHeader = sut.getHeaders(HttpHeaders.AUTHORIZATION);
        final List<String> headerList = Collections.list(authHeader);
        assertEquals(1, headerList.size());
        checkBasicAuthHeader(headerList.get(0));
    }

    @Test
    void getHeadersReturnsRequestHeaderForAuthorizationWhenUsernameIsNotConfigured() {
        final String authValue = "Bearer aaaaaa";
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, authValue);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, "", "");
        final Enumeration<String> authHeader = sut.getHeaders(HttpHeaders.AUTHORIZATION);
        final List<String> headerList = Collections.list(authHeader);
        assertEquals(1, headerList.size());
        assertEquals(authValue, headerList.get(0));
    }

    @Test
    void getHeaderNamesAddsAuthorizationWhenUsernameIsConfigured() {
        mockRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, USERNAME,
                PASSWORD);
        final Enumeration<String> result = sut.getHeaderNames();
        final List<String> resultAsString = Collections.list(result);
        assertTrue(resultAsString.contains(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void getHeadersReturnsRequestHeaderForNonAuthorization() {
        mockRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, USERNAME,
                PASSWORD);
        final Enumeration<String> result = sut.getHeaders(HttpHeaders.ACCEPT);
        final List<String> resultList = Collections.list(result);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, resultList.get(0));
    }

    @Test
    void getHeaderNamesDoesNotAddAuthorizationWhenUsernameIsNotConfigured() {
        mockRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        final AuthenticatingServletRequestWrapper sut = new AuthenticatingServletRequestWrapper(mockRequest, "", "");
        final Enumeration<String> result = sut.getHeaderNames();
        final List<String> resultAsString = Collections.list(result);
        assertFalse(resultAsString.contains(HttpHeaders.AUTHORIZATION));
    }
}
