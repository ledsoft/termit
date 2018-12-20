package cz.cvut.kbss.termit.service.export.util;

import org.springframework.core.io.ByteArrayResource;

/**
 * Adds support for media type and the associated file extension awareness to {@link ByteArrayResource}.
 */
public class TypeAwareResource extends ByteArrayResource {

    private final String mediaType;
    private final String fileExtension;

    public TypeAwareResource(byte[] byteArray, String mediaType, String fileExtension) {
        super(byteArray);
        this.mediaType = mediaType;
        this.fileExtension = fileExtension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
