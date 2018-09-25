package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.dto.LabelSearchResult;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
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
    public List<LabelSearchResult> searchByLabel(String searchString) {
        Objects.requireNonNull(searchString);
        return (List<LabelSearchResult>) em.createNativeQuery("SELECT * WHERE {" +
                "?x a ?type ;" +
                "?hasLabel ?label ." +
                "OPTIONAL {" +
                "?x ?inVocabulary ?vocabularyUri ." +
                "}" +
                "FILTER CONTAINS(LCASE(?label), LCASE(?searchString)) ." +
                "FILTER (?type = ?term || ?type = ?vocabulary) ." +
                "} ORDER BY ?label", "LabelSearchResult")
                                           .setParameter("hasLabel", URI.create(RDFS.LABEL))
                                           .setParameter("inVocabulary",
                                                   URI.create(Vocabulary.s_p_je_pojmem_ze_slovniku))
                                           .setParameter("term", URI.create(Vocabulary.s_c_term))
                                           .setParameter("vocabulary", URI.create(Vocabulary.s_c_slovnik))
                                           .setParameter("searchString", searchString, null).getResultList();
    }
}
