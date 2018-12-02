package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.Properties;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@OWLClass(iri = Vocabulary.s_c_term)
public class Term implements Serializable, HasTypes {

    /**
     * Names of columns used in term export.
     */
    public static final String[] EXPORT_COLUMNS = {"IRI", "Label", "Comment", "Types", "Sources", "SubTerms"};

    @Id
    private URI uri;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = RDFS.LABEL)
    private String label;

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @OWLDataProperty(iri = "http://purl.org/dc/elements/1.1/source")
    private Set<String> sources;

    @OWLObjectProperty(iri = Vocabulary.s_p_narrower, fetch = FetchType.EAGER)
    private Set<URI> subTerms;

    @Inferred
    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @Properties(fetchType = FetchType.EAGER)
    private Map<String, Set<String>> properties;

    @Types
    private Set<String> types;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Set<URI> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<URI> subTerms) {
        this.subTerms = subTerms;
    }

    public void addSubTerm(URI childUri) {
        Objects.requireNonNull(childUri);
        if (subTerms == null) {
            this.subTerms = new HashSet<>();
        }
        subTerms.add(childUri);
    }

    public Set<String> getSources() {
        return sources;
    }

    public void setSources(Set<String> source) {
        this.sources = source;
    }

    public URI getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(URI vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public Set<String> getTypes() {
        return types;
    }

    @Override
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Set<String>> properties) {
        this.properties = properties;
    }

    /**
     * Generates a CSV line representing this term.
     * <p>
     * The line contains:
     * <ul>
     * <li>IRI</li>
     * <li>Label</li>
     * <li>Comment</li>
     * <li>Types</li>
     * <li>Sources</li>
     * <li>Subterm IRIs</li>
     * </ul>
     *
     * @return CSV representation of this term
     */
    public String toCsv() {
        final StringBuilder sb = new StringBuilder(uri.toString());
        sb.append(',').append(CsvUtils.sanitizeString(label));
        sb.append(',').append(comment != null ? CsvUtils.sanitizeString(comment) : "");
        sb.append(',');
        if (types != null && !types.isEmpty()) {
            sb.append(exportCollection(types));
        }
        sb.append(',');
        if (sources != null && !sources.isEmpty()) {
            sb.append(exportCollection(sources));
        }
        sb.append(',');
        if (subTerms != null && !subTerms.isEmpty()) {
            sb.append(exportCollection(subTerms.stream().map(URI::toString).collect(Collectors.toSet())));
        }
        return sb.toString();
    }

    private String exportCollection(Collection<String> col) {
        return CsvUtils.sanitizeString("[" + String.join(";", col) + "]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Term)) {
            return false;
        }
        Term term = (Term) o;
        return Objects.equals(uri, term.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "Term{" +
                label +
                " <" + uri + '>' +
                ", types=" + types +
                '}';
    }
}
