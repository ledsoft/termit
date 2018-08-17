package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.Normalizer;
import java.util.Objects;

/**
 * Service for generating and resolving identifiers.
 */
@Service
public class IdentifierResolver {

    private final Configuration config;

    @Autowired
    public IdentifierResolver(Configuration config) {
        this.config = config;
    }

    /**
     * Normalizes the specified value which includes:
     * <ul>
     * <li>Transforming the value to lower case</li>
     * <li>Trimming the string</li>
     * <li>Replacing non-ASCII characters with ASCII, e.g., 'č' with 'c'</li>
     * <li>Replacing white spaces with dashes</li>
     * </ul>
     * <p>
     * Based on <a href="https://gist.github.com/rponte/893494">https://gist.github.com/rponte/893494</a>
     *
     * @param value The value to normalize
     * @return Normalized string
     */
    public static String normalize(String value) {
        Objects.requireNonNull(value);
        final String normalized = value.toLowerCase().trim().replace(' ', '-');
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Generates identifier, appending a normalized string consisting of the specified components to the namespace.
     *
     * @param namespace  Base URI for the generation
     * @param components Components to normalize and add to the identifier
     * @return Generated identifier
     */
    public URI generateIdentifier(String namespace, String... components) {
        Objects.requireNonNull(namespace);
        if (components.length == 0) {
            throw new IllegalArgumentException("Must provide at least one component for identifier generation.");
        }
        final String comps = String.join("-", components);
        if (!namespace.endsWith("/") && !namespace.endsWith("#")) {
            namespace += "/";
        }
        return URI.create(namespace + normalize(comps));
    }

    /**
     * Generates identifier, appending a normalized string consisting of the specified components to namespace
     * configured by the specified configuration parameter.
     *
     * @param namespaceConfig Configuration parameter for namespace
     * @param components      Components to normalize and add to the identifier
     * @return Generated identifier
     */
    public URI generateIdentifier(ConfigParam namespaceConfig, String... components) {
        Objects.requireNonNull(namespaceConfig);
        final String namespace = config.get(namespaceConfig);
        return generateIdentifier(namespace, components);
    }

    /**
     * Builds an identifier from the specified namespace and fragment.
     * <p>
     * This method assumes that the fragment is a normalized string uniquely identifying a resource in the specified
     * namespace.
     * <p>
     * Basically, the returned identifier should be the same as would be returned for non-normalized fragments by {@link
     * #generateIdentifier(String, String...)}.
     *
     * @param namespace Identifier namespace
     * @param fragment  Normalized string unique in the specified namespace
     * @return Identifier
     */
    public URI resolveIdentifier(String namespace, String fragment) {
        Objects.requireNonNull(namespace);
        if (!namespace.endsWith("/") && !namespace.endsWith("#")) {
            namespace += "/";
        }
        return URI.create(namespace + fragment);
    }

    /**
     * Builds an identifier from a namespace loaded from application configuration and the specified fragment.
     *
     * @param namespaceConfig Configuration parameter for loading namespace
     * @param fragment        Normalized string unique in the loaded namespace
     * @return Identifier
     * @see #resolveIdentifier(String, String)
     */
    public URI resolveIdentifier(ConfigParam namespaceConfig, String fragment) {
        Objects.requireNonNull(namespaceConfig);
        final String namespace = config.get(namespaceConfig);
        return resolveIdentifier(namespace, fragment);
    }

    /**
     * Extracts locally unique identifier fragment from the specified URI.
     *
     * @param uri URI to extract fragment from
     * @return Identification fragment
     */
    public static String extractIdentifierFragment(URI uri) {
        Objects.requireNonNull(uri);
        final String strUri = uri.toString();
        final int slashIndex = strUri.lastIndexOf('/');
        final int hashIndex = strUri.lastIndexOf('#');
        return strUri.substring((slashIndex > hashIndex ? slashIndex : hashIndex) + 1);
    }

    /**
     * Extracts namespace from the specified URI.
     * <p>
     * Namespace in this case means the part of the URI up to the last forward slash or hash tag, whichever comes
     * later.
     *
     * @param uri URI to extract namespace from
     * @return Identifier namespace
     */
    public static String extractIdentifierNamespace(URI uri) {
        final String strUri = uri.toString();
        final int slashIndex = strUri.lastIndexOf('/');
        final int hashIndex = strUri.lastIndexOf('#');
        return strUri.substring(0, (slashIndex > hashIndex ? slashIndex : hashIndex) + 1);
    }
}
