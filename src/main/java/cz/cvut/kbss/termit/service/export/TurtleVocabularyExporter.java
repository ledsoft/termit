package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.DataDao;
import cz.cvut.kbss.termit.util.Constants.Turtle;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service("turtle")
public class TurtleVocabularyExporter implements VocabularyExporter {

    private final DataDao dataDao;

    @Autowired
    public TurtleVocabularyExporter(DataDao dataDao) {
        this.dataDao = dataDao;
    }

    @Transactional
    @Override
    public TypeAwareResource exportVocabularyGlossary(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return dataDao.exportDataAsTurtle(vocabulary.getUri());
    }

    @Override
    public boolean supports(String mediaType) {
        return Objects.equals(Turtle.MEDIA_TYPE, mediaType);
    }
}
