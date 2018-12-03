package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Supports vocabulary export to MS Excel format
 */
@Service("excel")
public class ExcelVocabularyExporter implements VocabularyExporter {

    /**
     * Name of the single sheet produced by this exporter
     */
    static final String SHEET_NAME = "Glossary";

    private final TermRepositoryService termService;

    @Autowired
    public ExcelVocabularyExporter(TermRepositoryService termService) {
        this.termService = termService;
    }

    @Override
    public Resource exportVocabularyGlossary(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final HSSFWorkbook wb = new HSSFWorkbook();
        final HSSFSheet sheet = wb.createSheet(SHEET_NAME);
        generateHeaderRow(sheet);
        generateTermRows(termService.findAll(vocabulary.getUri(), Integer.MAX_VALUE, 0), sheet);
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return new ByteArrayResource(bos.toByteArray());
        } catch (IOException e) {
            throw new TermItException("Unable to generate excel file from glossary of " + vocabulary, e);
        }
    }

    private void generateHeaderRow(HSSFSheet sheet) {
        final HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < Term.EXPORT_COLUMNS.length; i++) {
            row.createCell(i).setCellValue(Term.EXPORT_COLUMNS[i]);
        }
    }

    private void generateTermRows(List<Term> terms, HSSFSheet sheet) {
        // Row no. 0 is the header
        for (int i = 0; i < terms.size(); i++) {
            final HSSFRow row = sheet.createRow(i + 1);
            terms.get(i).toExcel(row);
        }
    }
}
