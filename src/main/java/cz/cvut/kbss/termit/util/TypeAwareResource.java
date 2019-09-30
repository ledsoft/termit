package cz.cvut.kbss.termit.util;

import org.springframework.core.io.Resource;

import java.util.Optional;

/**
 * An IO resource aware of its media type.
 * <p>
 * Allows to get MIME type of the resource and the associated file extension. However, both methods return {@link Optional}
 * to accommodate resources which may not support this feature.
 */
public interface TypeAwareResource extends Resource {

    /**
     * Gets media type of this resource.
     *
     * @return MIME type wrapped in {@code Optional}
     */
    default Optional<String> getMediaType() {
        return Optional.empty();
    }

    /**
     * Gets file extension of this resource (if supported).
     *
     * @return File extension associated with this type of resource wrapped in {@code Optional}
     */
    default Optional<String> getFileExtension() {
        return Optional.empty();
    }
}
