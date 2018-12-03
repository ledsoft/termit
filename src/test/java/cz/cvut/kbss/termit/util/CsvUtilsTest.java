package cz.cvut.kbss.termit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilsTest {

    @Test
    void sanitizeStringReturnsNullIfNullIsPassedAsArgument() {
        assertNull(CsvUtils.sanitizeString(null));
    }

    @Test
    void sanitizeStringReturnsInputWhenNothingNeedsSanitization() {
        final String input = "This string needs no sanitization.";
        assertEquals(input, CsvUtils.sanitizeString(input));
    }

    @Test
    void sanitizeStringEnclosesStringInQuotesWhenItContainsCommas() {
        final String input = "string, which contains comma";
        final String result = CsvUtils.sanitizeString(input);
        assertEquals("\"" + input + "\"", result);
    }

    @Test
    void sanitizeStringDuplicatesDoubleQuotesInString() {
        final String input = "This string needs \"escaping\"";
        final String result = CsvUtils.sanitizeString(input);
        assertEquals("\"This string needs \"\"escaping\"\"\"", result);
    }
}