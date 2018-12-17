package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermTest {

    @Test
    void toCsvOutputsAllColumnsEvenIfNotAllAttributesArePresent() {
        final Term term = Generator.generateTermWithId();
        term.setComment(null);
        final String result = term.toCsv();
        int count = 0;
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == ',') {
                count++;
            }
        }
        assertEquals(5, count);
    }

    @Test
    void toCsvGeneratesStringContainingIriLabelComment() {
        final Term term = Generator.generateTermWithId();
        final String result = term.toCsv();
        final String[] items = result.split(",");
        assertThat(items.length, greaterThanOrEqualTo(3));
        assertEquals(term.getUri().toString(), items[0]);
        assertEquals(term.getLabel(), items[1]);
        assertEquals(term.getComment(), items[2]);
    }

    @Test
    void toCsvPutsCommentInQuotesToEscapeCommas() {
        final Term term = Generator.generateTermWithId();
        term.setComment("Comment, with a comma");
        final String result = term.toCsv();
        assertThat(result, containsString("\"" + term.getComment() + "\""));
    }

    @Test
    void toCsvExportsTypesInSquareBracketsDelimitedBySemicolons() {
        final Term term = Generator.generateTermWithId();
        term.setTypes(new LinkedHashSet<>(Arrays.asList(Vocabulary.s_c_object, Vocabulary.s_c_entity)));
        final String result = term.toCsv();
        final String[] items = result.split(",");
        assertThat(items.length, greaterThanOrEqualTo(4));
        final String types = items[3];
        assertTrue(types.matches("\\[.+;.+]"));
        term.getTypes().forEach(t -> assertTrue(types.contains(t)));
    }

    @Test
    void toCsvExportsSourcesInSquareBracketsDelimitedBySemicolons() {
        final Term term = Generator.generateTermWithId();
        term.setSources(new LinkedHashSet<>(
                Arrays.asList(Generator.generateUri().toString(), "PSP/c-1/p-2/b-c", "PSP/c-1/p-2/b-f")));
        final String result = term.toCsv();
        final String[] items = result.split(",");
        assertThat(items.length, greaterThanOrEqualTo(5));
        final String sources = items[4];
        assertTrue(sources.matches("\\[.+;.+]"));
        term.getSources().forEach(t -> assertTrue(sources.contains(t)));
    }

    @Test
    void toCsvExportsSubTermIrisInSquareBracketsDelimitedBySemicolons() {
        final Term term = Generator.generateTermWithId();
        term.setSubTerms(IntStream.range(0, 5).mapToObj(i -> Generator.generateUri()).collect(Collectors.toSet()));
        final String result = term.toCsv();
        final String[] items = result.split(",");
        assertThat(items.length, greaterThanOrEqualTo(6));
        final String subTerms = items[5];
        assertTrue(subTerms.matches("\\[.+;.+]"));
        term.getSubTerms().forEach(t -> assertTrue(subTerms.contains(t.toString())));
    }

    @Test
    void toExcelExportsTermToExcelRw() {
        final Term term = Generator.generateTermWithId();
        term.setTypes(Collections.singleton(Vocabulary.s_c_object));
        term.setSources(new LinkedHashSet<>(
                Arrays.asList(Generator.generateUri().toString(), "PSP/c-1/p-2/b-c", "PSP/c-1/p-2/b-f")));
        term.setSubTerms(IntStream.range(0, 5).mapToObj(i -> Generator.generateUri()).collect(Collectors.toSet()));
        final XSSFWorkbook wb = new XSSFWorkbook();
        final XSSFSheet sheet = wb.createSheet("test");
        final XSSFRow row = sheet.createRow(0);
        term.toExcel(row);
        assertEquals(term.getUri().toString(), row.getCell(0).getStringCellValue());
        assertEquals(term.getLabel(), row.getCell(1).getStringCellValue());
        assertEquals(term.getComment(), row.getCell(2).getStringCellValue());
        assertEquals(term.getTypes().iterator().next(), row.getCell(3).getStringCellValue());
        assertTrue(row.getCell(4).getStringCellValue().matches(".+;.+"));
        term.getSources().forEach(s -> assertTrue(row.getCell(4).getStringCellValue().contains(s)));
        assertTrue(row.getCell(5).getStringCellValue().matches(".+;.+"));
        term.getSubTerms().forEach(st -> assertTrue(row.getCell(5).getStringCellValue().contains(st.toString())));
    }

    @Test
    void toExcelHandlesEmptyOptionalAttributeValues() {
        final Term term = Generator.generateTermWithId();
        term.setComment(null);
        final XSSFWorkbook wb = new XSSFWorkbook();
        final XSSFSheet sheet = wb.createSheet("test");
        final XSSFRow row = sheet.createRow(0);
        term.toExcel(row);
        assertEquals(term.getUri().toString(), row.getCell(0).getStringCellValue());
        assertEquals(term.getLabel(), row.getCell(1).getStringCellValue());
        assertEquals(2, row.getLastCellNum());
    }

    @Test
    void toExcelHandlesSkippingEmptyColumns() {
        final Term term = Generator.generateTermWithId();
        term.setComment(null);
        term.setSources(new LinkedHashSet<>(
                Arrays.asList(Generator.generateUri().toString(), "PSP/c-1/p-2/b-c", "PSP/c-1/p-2/b-f")));
        final XSSFWorkbook wb = new XSSFWorkbook();
        final XSSFSheet sheet = wb.createSheet("test");
        final XSSFRow row = sheet.createRow(0);
        term.toExcel(row);
        assertEquals(term.getUri().toString(), row.getCell(0).getStringCellValue());
        assertEquals(term.getLabel(), row.getCell(1).getStringCellValue());
        assertTrue(row.getCell(4).getStringCellValue().matches(".+;.+"));
        term.getSources().forEach(s -> assertTrue(row.getCell(4).getStringCellValue().contains(s)));
    }

    @Test
    void toCsvSanitizesTermUriToHandleCommas() {
        final Term term = Generator.generateTerm();
        term.setUri(URI.create(
                "http://onto.fel.cvut.cz/ontologies/slovnik/oha-togaf/pojem/koncept-katalogů,-matic-a-pohledů"));
        final String result = term.toCsv();
        assertTrue(result.startsWith("\"" + term.getUri().toString() + "\","));
    }
}