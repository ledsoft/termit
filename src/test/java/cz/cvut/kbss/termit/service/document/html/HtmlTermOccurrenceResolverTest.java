package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.PropertyMockingApplicationContextInitializer;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(initializers = PropertyMockingApplicationContextInitializer.class)
class HtmlTermOccurrenceResolverTest extends BaseServiceTestRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private HtmlTermOccurrenceResolver sut;

    @Test
    void supportsReturnsTrueForFileWithHtmlLabelExtension() {
        final File file = new File();
        file.setLabel("rdfa-simple.html");
        assertTrue(sut.supports(file));
    }

    @Test
    void supportsReturnsTrueForFileWithHtmLabelExtension() {
        final File file = new File();
        file.setLabel("rdfa-simple.htm");
        assertTrue(sut.supports(file));
    }

    @Test
    void supportsReturnsTrueForHtmlFileWithoutExtension() throws Exception {
        final File file = generateFile();
        assertTrue(sut.supports(file));
    }

    private File generateFile() throws IOException {
        final java.io.File dir = Files.createTempDirectory("termit").toFile();
        dir.deleteOnExit();
        ((MockEnvironment) environment).setProperty(ConfigParam.FILE_STORAGE.toString(), dir.getAbsolutePath());
        final Document document = new Document();
        document.setLabel("testDocument");
        document.setUri(Generator.generateUri());
        final java.io.File docDir = new java.io.File(dir.getAbsolutePath() + java.io.File.separator +
                document.getDirectoryName());
        docDir.mkdir();
        docDir.deleteOnExit();
        final java.io.File content = Files.createTempFile(docDir.toPath(), "test", "").toFile();
        content.deleteOnExit();
        Files.copy(getClass().getClassLoader().getResourceAsStream("data/rdfa-simple.html"), content.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        final File file = new File();
        file.setLabel(content.getName());
        file.setDocument(document);
        document.addFile(file);
        return file;
    }
}