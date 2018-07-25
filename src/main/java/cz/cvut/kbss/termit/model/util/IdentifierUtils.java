package cz.cvut.kbss.termit.model.util;

import java.net.URI;
import java.text.Normalizer;
import java.util.Objects;

/**
 * Utility functions for generating entity identifiers.
 */
public class IdentifierUtils {

    /**
     * Normalizes the specified value which includes:
     * <ul>
     * <li>Transforming the value to lower case</li>
     * <li>Trimming the string</li>
     * <li>Replacing non-ASCII characters with ASCII, e.g., 'č' with 'c'</li>
     * <li>Replacing white spaces with dashes</li>
     * </ul>
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
     * Generates identifier, appending a normalized string consisting of the specified components to the base.
     *
     * @param base       Base URI for the generation
     * @param components Components to normalize and add to the identifier
     * @return Generated identifier
     */
    public static URI generateIdentifier(String base, String... components) {
        Objects.requireNonNull(base);
        if (components.length == 0) {
            throw new IllegalArgumentException("Must provide at least one component for identifier generation.");
        }
        final String comps = String.join("-", components);
        if (!base.endsWith("/")) {
            base += "/";
        }
        return URI.create(base + normalize(comps));
    }
}
