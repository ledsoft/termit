package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static cz.cvut.kbss.termit.environment.Environment.loadFile;
import static cz.cvut.kbss.termit.util.ConfigParam.FILE_STORAGE;
import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class DefaultDocumentManagerTest extends BaseServiceTestRunner {

    private static final String CONTENT =
            "<html><body><h1>Metropolitan plan</h1><p>Description of the metropolitan plan.</body></html>";

    @Autowired
    private Environment environment;

    @Autowired
    private DocumentManager sut;

    private Document document;

    @BeforeEach
    void setUp() {
        this.document = new Document();
        document.setName("Metropolitan plan");
        document.setUri(Generator.generateUri());
        document.setDateCreated(new Date());
    }

    private java.io.File generateFile() throws Exception {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final java.io.File docDir = new java.io.File(dir.getAbsolutePath() + java.io.File.separator +
                document.getFileDirectoryName());
        docDir.mkdir();
        docDir.deleteOnExit();
        final java.io.File content = Files.createTempFile(docDir.toPath(), "test", ".html").toFile();
        content.deleteOnExit();
        Files.write(content.toPath(), Collections.singletonList(CONTENT));
        return content;
    }

    @Test
    void loadFileContentThrowsNotFoundExceptionIfFileCannotBeFound() throws Exception {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final File file = new File();
        file.setFileName("unknown.html");
        document.addFile(file);
        assertThrows(NotFoundException.class, () -> sut.loadFileContent(document, file));
    }

    @Test
    void loadFileContentLoadsFileContentFromDisk() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        final String result = sut.loadFileContent(document, file);
        assertEquals(CONTENT, result);
    }

    @Test
    void getAsResourceReturnsResourceRepresentationOfFileOnDisk() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        final Resource result = sut.getAsResource(document, file);
        assertNotNull(result);
        assertEquals(physicalFile, result.getFile());
    }

    @Test
    void saveFileContentCreatesNewFileWhenNoneExists() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        physicalFile.delete();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        sut.saveFileContent(document, file, content);
        assertTrue(physicalFile.exists());
    }

    @Test
    void saveFileContentOverwritesExistingFileContent() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        sut.saveFileContent(document, file, content);
        final java.io.File contentFile = new java.io.File(
                environment.getProperty(FILE_STORAGE.toString()) + java.io.File.separator +
                        document.getFileDirectoryName() + java.io.File.separator + file.getFileName());
        final List<String> lines = Files.readAllLines(contentFile.toPath());
        final String result = String.join("\n", lines);
        assertFalse(result.isEmpty());
    }

    @Test
    void getMediaTypeResolvesMediaTypeOfFile() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        final Optional<String> result = sut.getMediaType(document, file);
        assertTrue(result.isPresent());
        assertEquals(MediaType.TEXT_HTML_VALUE, result.get());
    }

    @Test
    void createBackupCreatesBackupFileWithIdenticalContent() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setFileName(physicalFile.getName());
        document.addFile(file);
        final java.io.File docDir = physicalFile.getParentFile();
        assertNotNull(docDir.listFiles());
        assertEquals(1, docDir.listFiles().length);
        sut.createBackup(document, file);
        assertEquals(2, docDir.listFiles().length);
        for (java.io.File f : docDir.listFiles()) {
            f.deleteOnExit();
            assertEquals(CONTENT, String.join("\n", Files.readAllLines(f.toPath())));
        }
    }
}