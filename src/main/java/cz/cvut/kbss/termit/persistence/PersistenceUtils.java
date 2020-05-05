package cz.cvut.kbss.termit.persistence;

import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

@Component
public class PersistenceUtils {

    private final Configuration config;

    @Autowired
    public PersistenceUtils(Configuration config) {
        this.config = config;
    }

    /**
     * Determines the identifier of the repository context (named graph) in which vocabulary with the specified
     * identifier is stored.
     *
     * @param vocabularyUri Vocabulary identifier
     * @return Repository context identifier
     */
    public URI resolveVocabularyContext(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final String contextUri =
                vocabularyUri.toString() + config.get(ConfigParam.WORKING_VOCABULARY_CONTEXT_EXTENSION);
        return URI.create(contextUri);
    }
}
