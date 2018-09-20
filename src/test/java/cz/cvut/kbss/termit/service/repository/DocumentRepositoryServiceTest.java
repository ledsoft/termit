package cz.cvut.kbss.termit.service.repository;

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
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class DocumentRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private DocumentRepositoryService sut;

    private Document document;

    @BeforeEach
    void setUp() {
        this.document = new Document();
        document.setName("Metropolitan plan");
        document.setUri(Generator.generateUri());
    }

    @Test
    void resolveFileReturnsFileFromDisk() throws Exception {
        final File file = new File();
        final java.io.File content = generateFile();
        file.setFileName(content.getName());
        document.addFile(file);
        final java.io.File result = sut.resolveFile(document, file);
        assertEquals(content, result);
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
        return content;
    }

    @Test
    void resolveFileThrowsNotFoundExceptionIfFileCannotBeFound() throws Exception {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final File file = new File();
        file.setFileName("unknown.html");
        document.addFile(file);
        assertThrows(NotFoundException.class, () -> sut.resolveFile(document, file));
    }
}