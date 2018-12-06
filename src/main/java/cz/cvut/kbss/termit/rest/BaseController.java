package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.rest.util.RestUtils;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;

import static cz.cvut.kbss.termit.util.Constants.NAMESPACE_PARAM;

/**
 * Base for application REST controllers.
 * <p>
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

    /**
     * Resolves identifier based on the specified resource (if provided) or the namespace loaded from application
     * configuration.
     *
     * @param namespace       Explicitly provided namespace. Optional
     * @param fragment        Locally unique identifier fragment
     * @param namespaceConfig Namespace configuration parameter. Used in {@code namespace} is not specified
     * @return Resolved identifier
     */
    protected URI resolveIdentifier(String namespace, String fragment, ConfigParam namespaceConfig) {
        if (namespace != null) {
            return idResolver.resolveIdentifier(namespace, fragment);
        } else {
            return idResolver.resolveIdentifier(namespaceConfig, fragment);
        }
    }

    URI generateLocation(URI identifier, ConfigParam namespaceConfig) {
        if (identifier.toString().startsWith(config.get(namespaceConfig))) {
            return RestUtils.createLocationFromCurrentUriWithPath("/{name}",
                    IdentifierResolver.extractIdentifierFragment(identifier));
        } else {
            return RestUtils.createLocationFromCurrentUriWithPathAndQuery("/{name}", NAMESPACE_PARAM,
                    IdentifierResolver.extractIdentifierNamespace(identifier),
                    IdentifierResolver.extractIdentifierFragment(identifier));
        }
    }
}
