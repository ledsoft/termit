package cz.cvut.kbss.termit.rest.util;

import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Constants;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility functions for request processing.
 */
public class RestUtils {

    private RestUtils() {
        throw new AssertionError();
    }

    /**
     * Creates {@link HttpHeaders} object with location header corresponding to the current request's URI.
     *
     * @return {@code HttpHeaders} with location header
     */
    public static HttpHeaders createLocationHeaderFromCurrentUri() {
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, location.toASCIIString());
        return headers;
    }

    /**
     * Creates {@link HttpHeaders} object with a location header with the specified path appended to the current request
     * URI.
     * <p>
     * The {@code uriVariableValues} are used to fill in possible variables specified in {@code path}.
     *
     * @param path              Path to add to the current request URI in order to construct a resource location
     * @param uriVariableValues Values used to replace possible variables in the path
     * @return {@code HttpHeaders} with location header
     * @see #createLocationHeaderFromCurrentUriWithQueryParam(String, Object...)
     */
    public static HttpHeaders createLocationHeaderFromCurrentUriWithPath(String path, Object... uriVariableValues) {
        Objects.requireNonNull(path);
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path(path).buildAndExpand(
                uriVariableValues).toUri();
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, location.toASCIIString());
        return headers;
    }

    /**
     * Creates {@link HttpHeaders} object with a location header with the specified query parameter appended to the
     * current request URI.
     * <p>
     * The {@code values} are used as values of {@code param} in the resulting URI.
     *
     * @param param  Query parameter to add to current request URI
     * @param values Values of the query parameter
     * @return {@code HttpHeaders} with location header
     * @see #createLocationHeaderFromCurrentUriWithPath(String, Object...)
     */
    public static HttpHeaders createLocationHeaderFromCurrentUriWithQueryParam(String param, Object... values) {
        Objects.requireNonNull(param);
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().queryParam(param, values).build()
                                                        .toUri();
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, location.toASCIIString());
        return headers;
    }

    /**
     * Creates {@link HttpHeaders} object with a location header with the specified path and query parameter appended to
     * the current request URI.
     * <p>
     * The {@code paramValue} is specified for the query parameter and {@code pathValues} are used to replace path
     * variables.
     *
     * @param path       Path string, may contain path variables
     * @param param      Query parameter to add to current request URI
     * @param paramValue Value of the query parameter
     * @param pathValues Path variable values
     * @return {@code HttpHeaders} with location header
     * @see #createLocationHeaderFromCurrentUriWithPath(String, Object...)
     * @see #createLocationHeaderFromCurrentUriWithQueryParam(String, Object...)
     */
    public static HttpHeaders createLocationHeaderFromCurrentUriWithPathAndQuery(String path, String param,
                                                                                 Object paramValue,
                                                                                 Object... pathValues) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(param);
        final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().queryParam(param, paramValue)
                                                        .path(path).buildAndExpand(pathValues).toUri();
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, location.toASCIIString());
        return headers;
    }

    /**
     * Encodes the specifies value with an URL encoder, using {@link Constants#UTF_8_ENCODING}.
     *
     * @param value The value to encode
     * @return Encoded string
     */
    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, Constants.UTF_8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // Unlikely
            throw new TermItException("Encoding not found.", e);
        }
    }

    /**
     * Retrieves value of the specified cookie from the specified request.
     *
     * @param request    Request to get cookie from
     * @param cookieName Name of the cookie to retrieve
     * @return Value of the cookie
     */
    public static Optional<String> getCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return Optional.ofNullable(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }
}
