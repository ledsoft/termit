package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurtleVocabularyExporterTest extends VocabularyExporterTestBase {

    @Autowired
    private TurtleVocabularyExporter sut;

    @Test
    void supportsReturnsTrueForExcelMediaType() {
        assertTrue(sut.supports(Constants.Turtle.MEDIA_TYPE));
    }

    @Test
    void supportsReturnsFalseForNonExcelMediaType() {
        assertFalse(sut.supports(MediaType.APPLICATION_JSON_VALUE));
    }
}