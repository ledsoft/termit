package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;

/**
 * Manages the physical aspect of documents supported by the system, i.e., mainly the files stored for each document.
 * <p>
 * The object model of the system declares the notions of documents consisting of sets of files. These are all logical,
 * stored in the system's database. This class manages the physical aspect of these notions - the actual physical files
 * and their content.
 * <p>
 * Implementations may use various ways of storing the content of the files, ranging from files on file system to
 * document management systems.
 */
public interface DocumentManager {

    /**
     * Gets the content of the specified file.
     *
     * @param document Document containing the file. Used for path resolution
     * @param file     File representing the physical item
     * @return Content of the file
     * @throws NotFoundException If the file cannot be found
     */
    String loadFileContent(Document document, File file);

    /**
     * Gets the file as a {@link Resource}.
     * <p>
     * Resource is more suitable for sending the content over network.
     *
     * @param document Document containing the file. Used for path resolution
     * @param file     File representing the physical item
     * @return Resource representation of the physical item
     * @throws NotFoundException If the file cannot be found
     */
    Resource getAsResource(Document document, File file);

    /**
     * Resolves media type of the content of the specified file.
     *
     * @param document Document containing the file. Used for path resolution
     * @param file     File representing the physical item
     * @return Media type of the file content. If the file content does not represent any known media type, an empty
     * {@code Optional} is returned
     * @throws NotFoundException If the file cannot be found
     */
    Optional<String> getMediaType(Document document, File file);

    /**
     * Saves the specified content to a physical location represented by the specified file.
     * <p>
     * If no physical location exists for the specified file, a new one is created, otherwise the existing content is
     * overwritten by the new content.
     *
     * @param document Document containing the file. Used for path resolution
     * @param file     File representing the physical item
     * @param content  Content to save
     */
    void saveFileContent(Document document, File file, InputStream content);

    /**
     * Creates backup of the specified file.
     * <p>
     * Multiple backups of a file can be created and they should be distinguishable.
     *
     * @param document Document containing file. Used for path resolution
     * @param file     File to backup
     * @throws NotFoundException If the file cannot be found
     */
    void createBackup(Document document, File file);
}
