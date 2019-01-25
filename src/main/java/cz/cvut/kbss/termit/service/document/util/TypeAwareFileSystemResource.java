package cz.cvut.kbss.termit.service.document.util;

import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
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
}
