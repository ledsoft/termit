/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao.lucene;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.persistence.dao.SearchDao;
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

    public LuceneSearchDao(EntityManager em) {
        super(em);
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
