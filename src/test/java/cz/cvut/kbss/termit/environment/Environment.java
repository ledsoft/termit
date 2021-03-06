/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.environment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jsonld.ConfigParam;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.jsonld.jackson.JsonLdModule;
import cz.cvut.kbss.termit.dto.workspace.WorkspaceMetadata;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.workspace.WorkspaceMetadataCache;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static cz.cvut.kbss.termit.util.Constants.WORKSPACE_SESSION_ATT;

public class Environment {

    public static final String BASE_URI = Vocabulary.ONTOLOGY_IRI_termit;

    private static UserAccount currentUser;

    private static ObjectMapper objectMapper;

    private static ObjectMapper jsonLdObjectMapper;

    /**
     * Initializes security context with the specified user.
     *
     * @param user User to set as currently authenticated
     */
    public static void setCurrentUser(UserAccount user) {
        currentUser = user;
        final TermItUserDetails userDetails = new TermItUserDetails(user, new HashSet<>());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new AuthenticationToken(userDetails.getAuthorities(), userDetails));
        SecurityContextHolder.setContext(context);
    }

    /**
     * @see #setCurrentUser(UserAccount)
     */
    public static void setCurrentUser(User user) {
        final UserAccount ua = new UserAccount();
        ua.setUri(user.getUri());
        ua.setFirstName(user.getFirstName());
        ua.setLastName(user.getLastName());
        ua.setUsername(user.getUsername());
        ua.setTypes(user.getTypes());
        setCurrentUser(ua);
    }

    /**
     * Gets current user as security principal.
     *
     * @return Current user authentication as principal or {@code null} if there is no current user
     */
    public static Optional<Principal> getCurrentUserPrincipal() {
        return SecurityContextHolder.getContext() != null ?
               Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()) : Optional.empty();
    }

    public static UserAccount getCurrentUser() {
        return currentUser;
    }

    /**
     * Resets security context, removing any previously set data.
     */
    public static void resetCurrentUser() {
        currentUser = null;
        SecurityContextHolder.clearContext();
    }

    /**
     * Gets a Jackson {@link ObjectMapper} for mapping JSON to Java and vice versa.
     *
     * @return {@code ObjectMapper}
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // JSR 310 (Java 8 DateTime API)
            objectMapper.registerModule(new JavaTimeModule());
        }
        return objectMapper;
    }

    /**
     * Gets a Jackson {@link ObjectMapper} for mapping JSON-LD to Java and vice versa.
     *
     * @return {@code ObjectMapper}
     */
    public static ObjectMapper getJsonLdObjectMapper() {
        if (jsonLdObjectMapper == null) {
            jsonLdObjectMapper = new ObjectMapper();
            jsonLdObjectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
            jsonLdObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            jsonLdObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            final JsonLdModule module = new JsonLdModule();
            module.configure(ConfigParam.SCAN_PACKAGE, "cz.cvut.kbss.termit");
            jsonLdObjectMapper.registerModule(module);
        }
        return jsonLdObjectMapper;
    }

    /**
     * Creates a Jackson JSON-LD message converter.
     *
     * @return JSON-LD message converter
     */
    public static HttpMessageConverter<?> createJsonLdMessageConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
                getJsonLdObjectMapper());
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.valueOf(JsonLd.MEDIA_TYPE)));
        return converter;
    }

    public static HttpMessageConverter<?> createDefaultMessageConverter() {
        return new MappingJackson2HttpMessageConverter(getObjectMapper());
    }

    public static HttpMessageConverter<?> createStringEncodingMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    public static HttpMessageConverter<?> createResourceMessageConverter() {
        return new ResourceHttpMessageConverter();
    }

    public static InputStream loadFile(String file) {
        return Environment.class.getClassLoader().getResourceAsStream(file);
    }

    /**
     * Loads TermIt ontological model into the underlying repository, so that RDFS inference (mainly class and property
     * hierarchy) can be exploited.
     * <p>
     * Note that the specified {@code em} has to be transactional, so that a connection to the underlying repository is
     * open.
     *
     * @param em Transactional {@code EntityManager} used to unwrap the underlying repository
     */
    public static void addModelStructureForRdfsInference(EntityManager em) {
        final Repository repo = em.unwrap(Repository.class);
        try (final RepositoryConnection conn = repo.getConnection()) {
            conn.begin();
            conn.add(new URL(Vocabulary.ONTOLOGY_IRI_model_A), BASE_URI,
                    RDFFormat.TURTLE);
            conn.add(new File("ontology/termit-model.ttl"), BASE_URI, RDFFormat.TURTLE);
            // TODO Update the URI once the ontology is publicly available at its correct location
            conn.add(
                    new URL("https://raw.githubusercontent.com/opendata-mvcr/ssp/master/d-sgov/d-sgov-pracovn%C3%AD-prostor-0.0.1/d-sgov-pracovn%C3%AD-prostor-0.0.1-model.ttl"),
                    BASE_URI, RDFFormat.TURTLE);
            conn.add(new URL("http://www.w3.org/TR/skos-reference/skos.rdf"), "", RDFFormat.RDFXML);
            conn.commit();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load TermIt model for import.", e);
        }
    }

    /**
     * Sets the currently loaded workspace.
     *
     * @param workspace Workspace to set
     * @param ctx       Spring application context, used to retrieve relevant beans
     */
    public static void setCurrentWorkspace(Workspace workspace, ApplicationContext ctx) {
        final HttpSession session = ctx.getBean(HttpSession.class);
        final WorkspaceMetadataCache wsCache = ctx.getBean(WorkspaceMetadataCache.class);
        session.setAttribute(WORKSPACE_SESSION_ATT, workspace.getUri());
        wsCache.putWorkspace(new WorkspaceMetadata(workspace));
    }
}
