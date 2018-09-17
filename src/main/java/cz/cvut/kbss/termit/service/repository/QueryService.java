package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.util.Configuration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.ws.Response;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;

@Service public class QueryService {

    private final RestTemplate restClient;

    private final Configuration config;

    @Autowired public QueryService(RestTemplate restClient, Configuration config) {
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * Executes a SPARQL query.
     *
     * @param queryString the string representation of a SPARQL query
     */
    @Async public String query(String queryString) {
        final String repositoryUrl = config.get(REPOSITORY_URL);

        Objects.requireNonNull(queryString);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/ld+json");
        try {
            final JSONArray result = restClient
                .exchange((repositoryUrl + "?query={query}"),
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    JSONArray.class, queryString).getBody();
            return result.toString();
        } catch (RuntimeException e) {
            throw new WebServiceIntegrationException("Query invocation failed.", e);
        }
    }
}
