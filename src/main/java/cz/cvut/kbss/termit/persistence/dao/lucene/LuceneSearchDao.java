package cz.cvut.kbss.termit.persistence.dao.lucene;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.persistence.dao.SearchDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

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

    static final char LUCENE_WILDCARD = '*';

    private static final Logger LOG = LoggerFactory.getLogger(LuceneSearchDao.class);

    public LuceneSearchDao(EntityManager em) {
        super(em);
        LOG.debug("Instantiating LuceneSearchDao.");
    }

    @Override
    public List<FullTextSearchResult> fullTextSearch(String searchString) {
        Objects.requireNonNull(searchString);
        // Search string already contains a wildcard
        if (searchString.charAt(searchString.length() - 1) == LUCENE_WILDCARD) {
            return super.fullTextSearch(searchString);
        }
        // Append the last token also with a wildcard
        final String[] split = searchString.split("\\s+");
        final String lastTokenWithWildcard = split[split.length - 1] + LUCENE_WILDCARD;
        return super.fullTextSearch(String.join(" ", split) + " " + lastTokenWithWildcard);
    }
}
