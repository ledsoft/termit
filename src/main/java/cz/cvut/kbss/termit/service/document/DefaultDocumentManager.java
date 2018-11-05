package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default document manager uses files on filesystem to store content.
 */
@Service
public class DefaultDocumentManager implements DocumentManager {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDocumentManager.class);

    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd_HHmmss");

    private final Configuration config;

    @Autowired
    public DefaultDocumentManager(Configuration config) {
        this.config = config;
    }

    private java.io.File resolveFile(Document document, File file, boolean verifyExists) {
        Objects.requireNonNull(document);
        Objects.requireNonNull(file);
        final String path =
                config.get(ConfigParam.FILE_STORAGE) + java.io.File.separator + document.getFileDirectoryName() +
                        java.io.File.separator + file.getFileName();
        final java.io.File result = new java.io.File(path);
        if (verifyExists && !result.exists()) {
            LOG.error("File {} not found at location {}.", file, path);
            throw new NotFoundException("File " + file + " from document " + document + " not found on file system.");
        }
        return result;
    }

    @Override
    public String loadFileContent(Document document, File file) {
        try {
            final java.io.File content = resolveFile(document, file, true);
            LOG.debug("Loading file content from {}.", content);
            final List<String> lines = Files.readAllLines(content.toPath());
            return String.join("\n", lines);
        } catch (IOException e) {
            throw new TermItException("Unable to read file.", e);
        }
    }

    @Override
    public Resource getAsResource(Document document, File file) {
        return new FileSystemResource(resolveFile(document, file, true));
    }

    @Override
    public Optional<String> getMediaType(Document document, File file) {
        final java.io.File content = resolveFile(document, file, true);
        try {
            return Optional.ofNullable(Files.probeContentType(content.toPath()));
        } catch (IOException e) {
            throw new TermItException("Unable to determine file content type.", e);
        }
    }

    @Override
    public void saveFileContent(Document document, File file, InputStream content) {
        try {
            final java.io.File target = resolveFile(document, file, false);
            LOG.debug("Saving file content to {}.", target);
            Files.copy(content, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TermItException("Unable to write out file content.", e);
        }
    }

    @Override
    public void createBackup(Document document, File file) {
        try {
            final java.io.File toBackup = resolveFile(document, file, true);
            final java.io.File backupFile = new java.io.File(
                    toBackup.getParent() + java.io.File.separator + generateBackupFileName(file));
            LOG.debug("Backing up file {} to {}.", toBackup, backupFile);
            Files.copy(toBackup.toPath(), backupFile.toPath());
        } catch (IOException e) {
            throw new TermItException("Unable to backup file.", e);
        }
    }

    private String generateBackupFileName(File file) {
        final String origName = file.getFileName();
        final int dotIndex = origName.lastIndexOf('.');
        final String name = origName.substring(0, dotIndex > 0 ? dotIndex : origName.length());
        final String extension = dotIndex > 0 ? origName.substring(dotIndex) : "";
        return name + "~" + dateFormat.format(new Date()) + extension;
    }
}
