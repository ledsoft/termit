package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.util.TypeAwareResource;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static cz.cvut.kbss.termit.util.CsvUtils.FILE_EXTENSION;
import static cz.cvut.kbss.termit.util.CsvUtils.MEDIA_TYPE;

@Service("csv")
public class CsvVocabularyExporter implements VocabularyExporter {

    private final TermRepositoryService termService;

    @Autowired
    public CsvVocabularyExporter(TermRepositoryService termService) {
        this.termService = termService;
    }

    @Override
    public TypeAwareResource exportVocabularyGlossary(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final StringBuilder export = new StringBuilder(String.join(",", Term.EXPORT_COLUMNS));
        final List<Term> terms = termService.findAll(vocabulary);
        terms.forEach(t -> export.append('\n').append(t.toCsv()));
        return new TypeAwareResource(export.toString().getBytes(), MEDIA_TYPE, FILE_EXTENSION);
    }

    @Override
    public boolean supports(String mediaType) {
        return Objects.equals(MEDIA_TYPE, mediaType);
    }
}
