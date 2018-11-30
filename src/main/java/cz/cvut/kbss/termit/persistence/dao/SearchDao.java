package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class SearchDao {

    private final EntityManager em;

    @Autowired
    public SearchDao(EntityManager em) {
        this.em = em;
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
        return (List<FullTextSearchResult>) em.createNativeQuery("PREFIX : <http://www.ontotext.com/connectors/lucene#>\n" +
                "PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX pdp:<http://onto.fel.cvut.cz/ontologies/slovnik/agendovy/popis-dat/pojem/>\n" +
                "\n" +
                "SELECT DISTINCT ?entity ?label ?snippetText ?snippetField ?score ?type ?vocabularyURI {\n" +
                "  ?search1 a inst:czech_index ;\n" +
                "      :query ?searchString ;\n" +
                "      :entities ?entity .\n" +
                "  ?entity a ?type .\n" +
                "  ?entity rdfs:label ?label . \n" +
                "  ?entity :score ?score .\n" +
                "  ?entity :snippets _:s .\n" +
                "        _:s :snippetText ?snippetText .\n" +
                "    _:s :snippetField ?snippetField .\n" +
                "    OPTIONAL {\n" +
                "        ?entity pdp:je-pojmem-ze-slovniku ?vocabularyURI .\n" +
                "    }\n" +
                "    FILTER (?type = pdp:term || ?type = pdp:slovnik)\n" +
                "          \n" +
                "}\n" +
                "ORDER BY desc(?score)", "FullTextSearchResult")
                                           .setParameter("searchString", searchString, null).getResultList();
    }
}
