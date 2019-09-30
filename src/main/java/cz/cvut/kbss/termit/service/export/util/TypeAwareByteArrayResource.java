package cz.cvut.kbss.termit.service.export.util;

import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.core.io.ByteArrayResource;

import java.util.Objects;
import java.util.Optional;

/**
 * Adds support for media type and the associated file extension awareness to {@link ByteArrayResource}.
 */
public class TypeAwareByteArrayResource extends ByteArrayResource implements TypeAwareResource {

    private final String mediaType;
    private final String fileExtension;

    public TypeAwareByteArrayResource(byte[] byteArray, String mediaType, String fileExtension) {
        super(byteArray);
        this.mediaType = mediaType;
        this.fileExtension = fileExtension;
    }

    @Override
    public Optional<String> getMediaType() {
        return Optional.ofNullable(mediaType);
    }

    @Override
    public Optional<String> getFileExtension() {
        return Optional.ofNullable(fileExtension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeAwareByteArrayResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TypeAwareByteArrayResource that = (TypeAwareByteArrayResource) o;
        return Objects.equals(mediaType, that.mediaType) &&
                Objects.equals(fileExtension, that.fileExtension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mediaType, fileExtension);
    }
}
