package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.util.Constants.Turtle;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("turtle")
public class TurtleVocabularyExporter implements VocabularyExporter {

    @Override
    public TypeAwareResource exportVocabularyGlossary(Vocabulary vocabulary) {
        return null;
    }

    @Override
    public boolean supports(String mediaType) {
        return Objects.equals(Turtle.MEDIA_TYPE, mediaType);
    }
}
