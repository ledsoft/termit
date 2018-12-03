package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Term;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.util.Comparator;
import java.util.List;

import static cz.cvut.kbss.termit.service.export.ExcelVocabularyExporter.SHEET_NAME;
import static org.junit.jupiter.api.Assertions.*;

class ExcelVocabularyExporterTest extends VocabularyExporterTestBase {

    @Autowired
    private ExcelVocabularyExporter sut;

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Test
    void exportVocabularyGlossaryOutputsExcelWorkbookWithSingleSheet() throws Exception {
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        assertNotNull(result);
        final HSSFWorkbook wb = new HSSFWorkbook(result.getInputStream(), false);
        assertEquals(1, wb.getNumberOfSheets());
        assertEquals(0, wb.getSheetIndex(SHEET_NAME));
    }

    @Test
    void exportVocabularyGlossaryOutputsHeaderRowWithColumnNamesIntoSheet() throws Exception {
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        final HSSFWorkbook wb = new HSSFWorkbook(result.getInputStream(), false);
        final HSSFSheet sheet = wb.getSheet(SHEET_NAME);
        assertNotNull(sheet);
        final HSSFRow row = sheet.getRow(0);
        assertNotNull(row);
        for (int i = 0; i < Term.EXPORT_COLUMNS.length; i++) {
            assertEquals(Term.EXPORT_COLUMNS[i], row.getCell(i).getStringCellValue());
        }
    }

    @Test
    void exportVocabularyGlossaryOutputsGlossaryTermsIntoSheet() throws Exception {
        final List<Term> terms = generateTerms();
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        final HSSFWorkbook wb = new HSSFWorkbook(result.getInputStream(), false);
        final HSSFSheet sheet = wb.getSheet(SHEET_NAME);
        assertNotNull(sheet);
        // Plus header row
        assertEquals(terms.size(), sheet.getLastRowNum());
        for (int i = 1; i < sheet.getLastRowNum(); i++) {
            final HSSFRow row = sheet.getRow(i);
            final String id = row.getCell(0).getStringCellValue();
            assertTrue(terms.stream().anyMatch(t -> t.getUri().toString().equals(id)));
        }
    }

    @Test
    void exportVocabularyGlossaryOutputsGlossaryTermsOrderedByLabel() throws Exception {
        final List<Term> terms = generateTerms();
        terms.sort(Comparator.comparing(Term::getLabel));
        final Resource result = sut.exportVocabularyGlossary(vocabulary);
        final HSSFWorkbook wb = new HSSFWorkbook(result.getInputStream(), false);
        final HSSFSheet sheet = wb.getSheet(SHEET_NAME);
        assertNotNull(sheet);
        // Plus header row
        assertEquals(terms.size(), sheet.getLastRowNum());
        for (int i = 1; i < sheet.getLastRowNum(); i++) {
            final HSSFRow row = sheet.getRow(i);
            final String id = row.getCell(0).getStringCellValue();
            assertEquals(terms.get(i - 1).getUri().toString(), id);
        }
    }
}