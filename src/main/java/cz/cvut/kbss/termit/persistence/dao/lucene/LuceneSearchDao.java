package cz.cvut.kbss.termit.persistence.dao.lucene;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.persistence.dao.SearchDao;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * {@link SearchDao} extension for Lucene-based repositories. These support rich search strings with wildcards and
 * operators.
 * <p>
 * This DAO automatically adds a wildcard to the last token in the search string, so that results for incomplete words
 * are returned as well.
 */
@Repository
@Profile("lucene")  // Corresponds to a profile set in pom.xml
public class LuceneSearchDao extends SearchDao {

    private static final Logger LOG = LoggerFactory.getLogger(LuceneSearchDao.class);

    static final char LUCENE_WILDCARD = '*';

    private final Configuration config;

    public LuceneSearchDao(EntityManager em, Configuration config) {
        super(em);
        this.config = config;
    }

    @Override
    public List<FullTextSearchResult> fullTextSearch(String searchString) {
        Objects.requireNonNull(searchString);
        final String wildcardString = this.addWildcard(searchString);
        LOG.trace("Running full text search for search string \"{}\", using wildcard variant \"{}\".", searchString,
                wildcardString);
        return (List<FullTextSearchResult>) em.createNativeQuery(ftsQuery, "FullTextSearchResult")
                                              .setParameter("term", URI.create(Vocabulary.s_c_term))
                                              .setParameter("vocabulary", URI.create(Vocabulary.s_c_slovnik))
                                              .setParameter("inVocabulary",
                                                      URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                                              .setParameter("searchString", searchString, null)
                                              .setParameter("wildCardSearchString", wildcardString, null)
                                              .setParameter("langTag", config.get(ConfigParam.LANGUAGE), null)
                                              .getResultList();
    }

    private String addWildcard(String searchString) {
        // Search string already contains a wildcard
        if (searchString.charAt(searchString.length() - 1) == LUCENE_WILDCARD) {
            return searchString;
        }
        // Append the last token also with a wildcard
        final String[] split = searchString.split("\\s+");
        final String lastTokenWithWildcard = split[split.length - 1] + LUCENE_WILDCARD;
        return String.join(" ", split) + " " + lastTokenWithWildcard;
    }
}
