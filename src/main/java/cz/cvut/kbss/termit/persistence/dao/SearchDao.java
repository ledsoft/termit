package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class SearchDao {

    private static final String FTS_QUERY_FILE = "fulltextsearch.rq";

    private static final Logger LOG = LoggerFactory.getLogger(SearchDao.class);

    private String ftsQuery;

    private final EntityManager em;

    @Autowired
    public SearchDao(EntityManager em) {
        this.em = em;
    }

    @PostConstruct
    private void loadQueries() {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(
                Constants.QUERY_DIRECTORY + File.separator + FTS_QUERY_FILE);
        if (is == null) {
            throw new TermItException(
                    "Initialization exception. Full text search query not found in " + Constants.QUERY_DIRECTORY +
                            File.separator + FTS_QUERY_FILE);
        }
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            this.ftsQuery = in.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new TermItException("Initialization exception. Unable to load full text search query!", e);
        }
    }

    /**
     * Finds terms and vocabularies whose label (name) matches the specified search string.
     * <p>
     * Note that currently the match is done using simple contains on lower case strings.
     *
     * @param searchString The string to search by
     * @return List of matching results
     */
    public List<FullTextSearchResult> fullTextSearch(String searchString) {
        Objects.requireNonNull(searchString);
        LOG.trace("Running full text search for search string \"{}\".", searchString);
        return (List<FullTextSearchResult>) em.createNativeQuery(ftsQuery, "FullTextSearchResult")
                                              .setParameter("term", URI.create(Vocabulary.s_c_term))
                                              .setParameter("vocabulary", URI.create(Vocabulary.s_c_slovnik))
                                              .setParameter("inVocabulary",
                                                      URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                                              .setParameter("searchString", searchString, null).getResultList();
    }
}
