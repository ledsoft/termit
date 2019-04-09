package cz.cvut.kbss.termit.service.document.util;

import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * File system resource aware of its type.
 */
public class TypeAwareFileSystemResource extends FileSystemResource implements TypeAwareResource {

    private final String mediaType;

    public TypeAwareFileSystemResource(File file, String mediaType) {
        super(file);
        this.mediaType = mediaType;
    }

    @Override
    public Optional<String> getMediaType() {
        return Optional.ofNullable(mediaType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeAwareFileSystemResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TypeAwareFileSystemResource that = (TypeAwareFileSystemResource) o;
        return Objects.equals(mediaType, that.mediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mediaType);
    }
}
