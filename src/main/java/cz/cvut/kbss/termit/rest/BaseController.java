package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.rest.util.RestUtils;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;

/**
 * Base for application REST controllers.
 *
 * Will be used to define general security for the public API.
 */
@PreAuthorize("hasRole('" + SecurityConstants.ROLE_USER + "')")
public class BaseController {

    final IdentifierResolver idResolver;

    private final Configuration config;

    protected BaseController(IdentifierResolver idResolver, Configuration config) {
        this.idResolver = idResolver;
        this.config = config;
    }

    HttpHeaders generateLocationHeader(URI identifier, ConfigParam namespaceConfig) {
        if (identifier.toString().startsWith(config.get(namespaceConfig))) {
            return RestUtils.createLocationHeaderFromCurrentUriWithPath("/{name}",
                    IdentifierResolver.extractIdentifierFragment(identifier));
        } else {
            return RestUtils.createLocationHeaderFromCurrentUriWithPathAndQuery("/{name}", "namespace",
                    IdentifierResolver.extractIdentifierNamespace(identifier),
                    IdentifierResolver.extractIdentifierFragment(identifier));
        }
    }
}
