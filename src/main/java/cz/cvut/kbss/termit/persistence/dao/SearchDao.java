/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cz.cvut.kbss.termit.model.util.EntityToOwlClassMapper.getOwlClassForEntity;

@Repository
@Profile("!lucene")
public class SearchDao {

    private static final String FTS_QUERY_FILE = "fulltextsearch.rq";

    private static final Logger LOG = LoggerFactory.getLogger(SearchDao.class);

    protected String ftsQuery;

    protected final EntityManager em;

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
     * Finds terms and vocabularies which match the specified search string.
     * <p>
     * The search functionality depends on the underlying repository and the index it uses. But basically the search
     * looks for match in asset label, comment and SKOS definition.
     *
     * @param searchString The string to search by
     * @return List of matching results
     */
    public List<FullTextSearchResult> fullTextSearch(String searchString) {
        Objects.requireNonNull(searchString);
        LOG.trace("Running full text search for search string \"{}\".", searchString);
        return (List<FullTextSearchResult>) em.createNativeQuery(ftsQuery, "FullTextSearchResult")
                                              .setParameter("term", URI.create(getOwlClassForEntity(Term.class)))
                                              .setParameter("vocabulary", URI.create(getOwlClassForEntity(
                                                      cz.cvut.kbss.termit.model.Vocabulary.class)))
                                              .setParameter("inVocabulary",
                                                      URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                                              .setParameter("searchString", searchString, null).getResultList();
    }
}
