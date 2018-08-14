package cz.cvut.kbss.termit.rest.util;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;

class RestUtilsTest {

    @Test
    void createLocationHeaderFromCurrentUriWithPathAddsPathWithVariableReplacementsToRequestUri() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest(HttpMethod.GET.toString(),
                "/vocabularies");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        final String id = "117";

        final HttpHeaders result = RestUtils.createLocationHeaderFromCurrentUriWithPath("/{id}", id);
        assertTrue(result.containsKey(HttpHeaders.LOCATION));
        final String location = result.getLocation().toString();
        assertThat(location, endsWith("/vocabularies/" + id));
    }

    @Test
    void createLocationHeaderFromCurrentUriWithQueryParamAddsQueryParameterWithValueToRequestUri() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest(HttpMethod.GET.toString(),
                "/vocabularies");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        final URI id = Generator.generateUri();

        final HttpHeaders result = RestUtils.createLocationHeaderFromCurrentUriWithQueryParam("id", id);
        assertTrue(result.containsKey(HttpHeaders.LOCATION));
        final String location = result.getLocation().toString();
        assertThat(location, endsWith("/vocabularies?id=" + id));
    }

    @Test
    void getCookieExtractsCookieValueFromRequest() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest(HttpMethod.GET.toString(),
                "/vocabularies");
        mockRequest.setCookies(new Cookie(SecurityConstants.REMEMBER_ME_COOKIE_NAME, Boolean.TRUE.toString()));

        final Optional<String> result = RestUtils.getCookie(mockRequest, SecurityConstants.REMEMBER_ME_COOKIE_NAME);
        assertTrue(result.isPresent());
        assertTrue(Boolean.parseBoolean(result.get()));
    }

    @Test
    void getCookieReturnsEmptyOptionalWhenCookieIsNotFound() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest(HttpMethod.GET.toString(),
                "/vocabularies");
        mockRequest.setCookies(new Cookie(SecurityConstants.REMEMBER_ME_COOKIE_NAME, Boolean.TRUE.toString()));

        final Optional<String> result = RestUtils.getCookie(mockRequest, "unknown-cookie");
        assertFalse(result.isPresent());
    }

    @Test
    void urlEncodeEncodesSpecifiedStringWithUTF8URLEncoding() throws Exception {
        final String value = Generator.generateUri().toString();
        assertEquals(URLEncoder.encode(value, Constants.UTF_8_ENCODING), RestUtils.urlEncode(value));
    }
}