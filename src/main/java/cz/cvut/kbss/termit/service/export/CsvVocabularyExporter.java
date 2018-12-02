package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service("csv")
public class CsvVocabularyExporter implements VocabularyExporter {

    private final TermRepositoryService termService;

    @Autowired
    public CsvVocabularyExporter(TermRepositoryService termService) {
        this.termService = termService;
    }

    @Override
    public Resource exportVocabularyGlossary(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final StringBuilder export = new StringBuilder(String.join(",", Term.EXPORT_COLUMNS));
        final List<Term> terms = termService.findAll(vocabulary.getUri(), Integer.MAX_VALUE, 0);
        terms.forEach(t -> export.append('\n').append(t.toCsv()));
        return new ByteArrayResource(export.toString().getBytes());
    }
}
