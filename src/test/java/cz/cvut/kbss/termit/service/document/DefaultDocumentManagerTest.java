/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.TypeAwareResource;
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
        document.setLabel("Metropolitan plan");
        document.setUri(Generator.generateUri());
        document.setCreated(new Date());
    }

    private java.io.File generateFile() throws Exception {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final java.io.File docDir = new java.io.File(dir.getAbsolutePath() + java.io.File.separator +
                document.getDirectoryName());
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
        file.setLabel("unknown.html");
        document.addFile(file);
        file.setDocument(document);
        assertThrows(NotFoundException.class, () -> sut.loadFileContent(file));
    }

    @Test
    void loadFileContentLoadsFileContentFromDisk() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        final String result = sut.loadFileContent(file);
        assertEquals(CONTENT, result);
    }

    @Test
    void loadFileContentSupportsFileWithoutParentDocument() throws Exception {
        final File file = new File();
        file.setLabel("test-file.html");
        file.setUri(Generator.generateUri());
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final java.io.File fileDir = new java.io.File(dir.getAbsolutePath() + java.io.File.separator +
                file.getDirectoryName());
        fileDir.mkdir();
        fileDir.deleteOnExit();
        final java.io.File content = new java.io.File(fileDir + java.io.File.separator + file.getLabel());
        content.deleteOnExit();
        Files.write(content.toPath(), Collections.singletonList(CONTENT));

        final String result = sut.loadFileContent(file);
        assertEquals(CONTENT, result);
    }

    @Test
    void getAsResourceReturnsResourceRepresentationOfFileOnDisk() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        final Resource result = sut.getAsResource(file);
        assertNotNull(result);
        assertEquals(physicalFile, result.getFile());
    }

    @Test
    void getAsResourceReturnsResourceAwareOfMediaType() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        final TypeAwareResource result = sut.getAsResource(file);
        assertTrue(result.getMediaType().isPresent());
        assertEquals(MediaType.TEXT_HTML_VALUE, result.getMediaType().get());
    }

    @Test
    void saveFileContentCreatesNewFileWhenNoneExists() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        physicalFile.delete();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        sut.saveFileContent(file, content);
        assertTrue(physicalFile.exists());
    }

    @Test
    void saveFileContentOverwritesExistingFileContent() throws Exception {
        final InputStream content = loadFile("data/rdfa-simple.html");
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        sut.saveFileContent(file, content);
        final java.io.File contentFile = new java.io.File(
                environment.getProperty(FILE_STORAGE.toString()) + java.io.File.separator +
                        document.getDirectoryName() + java.io.File.separator + file.getLabel());
        final List<String> lines = Files.readAllLines(contentFile.toPath());
        final String result = String.join("\n", lines);
        assertFalse(result.isEmpty());
    }

    @Test
    void createBackupCreatesBackupFileWithIdenticalContent() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        final java.io.File docDir = physicalFile.getParentFile();
        assertNotNull(docDir.listFiles());
        assertEquals(1, docDir.listFiles().length);
        sut.createBackup(file);
        assertEquals(2, docDir.listFiles().length);
        for (java.io.File f : docDir.listFiles()) {
            f.deleteOnExit();
            assertEquals(CONTENT, String.join("\n", Files.readAllLines(f.toPath())));
        }
    }

    @Test
    void createBackupCreatesBackupOfFileWithoutExtension() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        final java.io.File docDir = physicalFile.getParentFile();
        final java.io.File withoutExtension = new java.io.File(
                docDir.getAbsolutePath() + java.io.File.separator + "withoutExtension");
        withoutExtension.deleteOnExit();
        Files.copy(physicalFile.toPath(), withoutExtension.toPath());
        file.setLabel(withoutExtension.getName());
        document.addFile(file);
        file.setDocument(document);
        assertNotNull(docDir.listFiles());
        sut.createBackup(file);
        final java.io.File[] files = docDir.listFiles((d, name) -> name.startsWith("withoutExtension"));
        assertEquals(2, files.length);
        for (java.io.File f : files) {
            f.deleteOnExit();
            assertEquals(CONTENT, String.join("\n", Files.readAllLines(f.toPath())));
        }
    }

    @Test
    void existsReturnsTrueForExistingFile() throws Exception {
        final File file = new File();
        final java.io.File physicalFile = generateFile();
        file.setLabel(physicalFile.getName());
        document.addFile(file);
        file.setDocument(document);
        assertTrue(sut.exists(file));
    }

    @Test
    void existsReturnsFalseForNonExistentFile() {
        final File file = new File();
        file.setLabel("test.html");
        document.addFile(file);
        file.setDocument(document);
        assertFalse(sut.exists(file));
    }
}