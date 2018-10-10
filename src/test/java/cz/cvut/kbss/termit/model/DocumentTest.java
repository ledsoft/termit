package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.service.IdentifierResolver;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    void getFileDirectoryNameReturnsNameBasedOnNormalizedNameAndUriHash() {
        final Document document = new Document();
        document.setName("Metropolitan plan");
        document.setUri(URI.create("http://onto.fel.cvut.cz/ontologies/termit/vocabulary/metropolitan-plan"));
        final String result = document.getFileDirectoryName();
        assertNotNull(result);
        assertThat(result, startsWith(IdentifierResolver.normalize(document.getName())));
    }

    @Test
    void getFileDirectoryNameThrowsIllegalStateWhenNameIsMissing() {
        final Document document = new Document();
        document.setUri(URI.create("http://onto.fel.cvut.cz/ontologies/termit/vocabulary/metropolitan-plan"));
        assertThrows(IllegalStateException.class, document::getFileDirectoryName);
    }

    @Test
    void getFileDirectoryNameThrowsIllegalStateWhenUriIsMissing() {
        final Document document = new Document();
        document.setName("Metropolitan plan");
        assertThrows(IllegalStateException.class, document::getFileDirectoryName);
    }

    @Test
    void getFileReturnsOptionalWithFileWithMatchingName() {
        final Document document = new Document();
        document.setName("Metropolitan plan");
        final File fOne = new File();
        fOne.setFileName("test1.html");
        document.addFile(fOne);
        final File fTwo = new File();
        fTwo.setFileName("test2.html");
        document.addFile(fTwo);
        final Optional<File> result = document.getFile(fOne.getFileName());
        assertTrue(result.isPresent());
        assertSame(fOne, result.get());
    }

    @Test
    void getFileReturnsEmptyOptionalForUnknownFileName() {
        final Document document = new Document();
        document.setName("Metropolitan plan");
        final File fOne = new File();
        fOne.setFileName("test1.html");
        document.addFile(fOne);
        assertFalse(document.getFile("unknown.html").isPresent());
    }

    @Test
    void getFileReturnsEmptyOptionalForDocumentWithoutFiles() {
        final Document document = new Document();
        document.setName("Metropolitan plan");
        assertFalse(document.getFile("unknown.html").isPresent());
    }
}