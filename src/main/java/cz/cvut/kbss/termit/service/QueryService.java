package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.WebServiceIntegrationException;
import cz.cvut.kbss.termit.util.Configuration;

import java.util.Objects;

import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static cz.cvut.kbss.termit.util.ConfigParam.REPOSITORY_URL;

@Service
public class QueryService {

    private final RestTemplate restClient;

    private final Configuration config;

    @Autowired
    public QueryService(RestTemplate restClient, Configuration config) {
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * Executes a SPARQL query.
     *
     * @param queryString the string representation of a SPARQL query
     */
    public String query(String queryString) {
        final String repositoryUrl = config.get(REPOSITORY_URL);

        Objects.requireNonNull(queryString);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, JsonLd.MEDIA_TYPE);
        try {
            final JSONArray result = restClient
                    .exchange((repositoryUrl + "?query={query}"),
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            JSONArray.class, queryString).getBody();
            return result.toJSONString();
        } catch (RuntimeException e) {
            throw new WebServiceIntegrationException("Query invocation failed.", e);
        }
    }
}
