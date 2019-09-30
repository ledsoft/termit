package cz.cvut.kbss.termit.dto.assignment;


import cz.cvut.kbss.jopa.model.annotations.ConstructorResult;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.SparqlResultSetMapping;
import cz.cvut.kbss.jopa.model.annotations.VariableResult;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;

/**
 * Represents aggregated information about a Term occurring in a Resource.
 * <p>
 * It contains info about the Resource - identifier and label - and how many times the Term occurs in it.
 */
@SparqlResultSetMapping(name = "TermOccurrences", classes = @ConstructorResult(
        targetClass = TermOccurrences.class,
        variables = {
                @VariableResult(name = "term", type = URI.class),
                @VariableResult(name = "resource", type = URI.class),
                @VariableResult(name = "label", type = String.class),
                @VariableResult(name = "cnt", type = Integer.class),
                @VariableResult(name = "suggested", type = Boolean.class)
        }
))
public class TermOccurrences extends TermAssignments {

    public static final String COUNT_PROPERTY = "http://onto.fel.cvut.cz/ontologies/application/termit/pojem/počet";

    @OWLDataProperty(iri = COUNT_PROPERTY)
    private Integer count;

    public TermOccurrences() {
    }

    public TermOccurrences(URI term, URI resource, String resourceLabel, Integer count, Boolean suggested) {
        super(term, resource, resourceLabel, suggested);
        this.count = count;
        addType(Vocabulary.s_c_vyskyt_termu);
        if (suggested) {
            addType(Vocabulary.s_c_navrzeny_vyskyt_termu);
        }
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TermOccurrences)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TermOccurrences that = (TermOccurrences) o;
        return Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), count);
    }

    @Override
    public String toString() {
        return "TermOccurrences{" +
                super.toString() +
                "count=" + count +
                "}";
    }
}
