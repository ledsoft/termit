package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class VocabularyExporters {

    private final VocabularyExporter csvExporter;

    private final VocabularyExporter excelExporter;

    public VocabularyExporters(@Qualifier("csv") VocabularyExporter csvExporter,
                               @Qualifier("excel") VocabularyExporter excelExporter) {
        this.csvExporter = csvExporter;
        this.excelExporter = excelExporter;
    }

    /**
     * @see CsvVocabularyExporter
     */
    public Resource exportVocabularyGlossaryToCsv(Vocabulary vocabulary) {
        return csvExporter.exportVocabularyGlossary(vocabulary);
    }

    /**
     * @see ExcelVocabularyExporter
     */
    public Resource exportVocabularyGlossaryToExcel(Vocabulary vocabulary) {
        return excelExporter.exportVocabularyGlossary(vocabulary);
    }
}
