package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.util.TypeAwareFileSystemResource;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private java.io.File resolveFile(File file, boolean verifyExists) {
        Objects.requireNonNull(file);
        final String path =
                config.get(ConfigParam.FILE_STORAGE) + java.io.File.separator + file.getDirectoryName() +
                        java.io.File.separator + IdentifierResolver.sanitizeFileName(file.getLabel());
        final java.io.File result = new java.io.File(path);
        if (verifyExists && !result.exists()) {
            LOG.error("File {} not found at location {}.", file, path);
            throw new NotFoundException("File " + file + " not found on file system.");
        }
        return result;
    }

    @Override
    public String loadFileContent(File file) {
        try {
            final java.io.File content = resolveFile(file, true);
            LOG.debug("Loading file content from {}.", content);
            final List<String> lines = Files.readAllLines(content.toPath());
            return String.join("\n", lines);
        } catch (IOException e) {
            throw new TermItException("Unable to read file.", e);
        }
    }

    @Override
    public TypeAwareResource getAsResource(File file) {
        return new TypeAwareFileSystemResource(resolveFile(file, true), getMediaType(file));
    }

    private String getMediaType(File file) {
        final java.io.File content = resolveFile(file, true);
        try {
            return Files.probeContentType(content.toPath());
        } catch (IOException e) {
            throw new TermItException("Unable to determine file content type.", e);
        }
    }

    @Override
    public void saveFileContent(File file, InputStream content) {
        try {
            final java.io.File target = resolveFile(file, false);
            LOG.debug("Saving file content to {}.", target);
            target.getParentFile().mkdirs();
            Files.copy(content, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TermItException("Unable to write out file content.", e);
        }
    }

    @Override
    public void createBackup(File file) {
        try {
            final java.io.File toBackup = resolveFile(file, true);
            final java.io.File backupFile = new java.io.File(
                    toBackup.getParent() + java.io.File.separator + generateBackupFileName(file));
            LOG.debug("Backing up file {} to {}.", toBackup, backupFile);
            Files.copy(toBackup.toPath(), backupFile.toPath());
        } catch (IOException e) {
            throw new TermItException("Unable to backup file.", e);
        }
    }

    private String generateBackupFileName(File file) {
        final String origName = IdentifierResolver.sanitizeFileName(file.getLabel());
        final int dotIndex = origName.lastIndexOf('.');
        final String name = origName.substring(0, dotIndex > 0 ? dotIndex : origName.length());
        final String extension = dotIndex > 0 ? origName.substring(dotIndex) : "";
        return name + "~" + dateFormat.format(new Date()) + extension;
    }

    @Override
    public boolean exists(File file) {
        return resolveFile(file, false).exists();
    }

    @Override
    public Optional<String> getContentType(File file) {
        final java.io.File physicalFile = resolveFile(file, true);
        try {
            return Optional.ofNullable(Files.probeContentType(physicalFile.toPath()));
        } catch (IOException e) {
            LOG.error("Exception caught when determining content type of file {}.", file, e);
            return Optional.empty();
        }
    }
}
