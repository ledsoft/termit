package cz.cvut.kbss.termit.model.resource;

import cz.cvut.kbss.termit.environment.Generator;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileTest {

    @Test
    void getDirectoryNameReturnsParentDocumentDirectoryWhenDocumentIsReferenced() {
        final File sut = new File();
        sut.setLabel("text-mpp.html");
        sut.setUri(Generator.generateUri());
        final Document parent = new Document();
        parent.setUri(Generator.generateUri());
        parent.setLabel("Parent document");
        sut.setDocument(parent);
        parent.addFile(sut);
        assertEquals(parent.getDirectoryName(), sut.getDirectoryName());
    }

    @Test
    void getDirectoryNameReturnsDirectoryNameDerivedFromFileNameWhenParentDocumentIsNotSet() {
        final File sut = new File();
        sut.setLabel("text-mpp.html");
        sut.setUri(Generator.generateUri());
        final String result = sut.getDirectoryName();
        assertThat(result, containsString(sut.getLabel().substring(0, sut.getLabel().indexOf('.'))));
        assertThat(result, containsString(Integer.toString(sut.getUri().hashCode())));
    }

    @Test
    void getDirectoryNameDoesNotContainFileExtension() {
        final File sut = new File();
        sut.setLabel("text-mpp.html");
        sut.setUri(Generator.generateUri());
        final String result = sut.getDirectoryName();
        assertThat(result, not(containsString(".html")));
    }

    @Test
    void getDirectoryNameThrowsIllegalStateExceptionWhenFileLabelIsMissing() {
        final File sut = new File();
        sut.setUri(Generator.generateUri());
        assertThrows(IllegalStateException.class, sut::getDirectoryName);
    }

    @Test
    void getDirectoryNameThrowsIllegalStateExceptionWhenFileUriIsMissing() {
        final File sut = new File();
        sut.setLabel("text-mpp.html");
        assertThrows(IllegalStateException.class, sut::getDirectoryName);
    }
}