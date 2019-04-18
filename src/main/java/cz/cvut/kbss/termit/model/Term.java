package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.Properties;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.poi.ss.usermodel.Row;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@OWLClass(iri = Vocabulary.s_c_term)
@JsonLdAttributeOrder({"uri", "label", "comment", "author", "lastEditor"})
@EntityListeners(ProvenanceManager.class)
public class Term extends Asset implements HasTypes, Serializable {

    /**
     * Names of columns used in term export.
     */
    public static final List<String> EXPORT_COLUMNS = Collections
            .unmodifiableList(Arrays.asList("IRI", "Label", "Definition", "Comment", "Types", "Sources", "SubTerms"));

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    // TODO Can be replaced by JOPA SKOS.DEFINITION constant, available in the next eversion
    @OWLAnnotationProperty(iri = Vocabulary.s_p_definition)
    private String definition;

    @OWLDataProperty(iri = DC.Elements.SOURCE)
    private Set<String> sources;

    // TODO Replace with a reference to JOPA SKOS, which will be available in the next version
    @OWLObjectProperty(iri = Constants.SKOS_NARROWER, fetch = FetchType.EAGER)
    private Set<URI> subTerms;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @Properties(fetchType = FetchType.EAGER)
    private Map<String, Set<String>> properties;

    @Types
    private Set<String> types;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Set<URI> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<URI> subTerms) {
        this.subTerms = subTerms;
    }

    public boolean addSubTerm(URI childUri) {
        Objects.requireNonNull(childUri);
        if (subTerms == null) {
            this.subTerms = new HashSet<>();
        }
        return subTerms.add(childUri);
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
     * The line contains columns specified in {@link #EXPORT_COLUMNS}
     *
     * @return CSV representation of this term
     */
    public String toCsv() {
        final StringBuilder sb = new StringBuilder(CsvUtils.sanitizeString(getUri().toString()));
        sb.append(',').append(CsvUtils.sanitizeString(getLabel()));
        sb.append(',').append(definition != null ? CsvUtils.sanitizeString(definition) : "");
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

    private static String exportCollection(Collection<String> col) {
        return CsvUtils.sanitizeString(String.join(";", col));
    }

    /**
     * Generates an Excel line (line with tab separated values) representing this term.
     * <p>
     * The line contains columns specified in {@link #EXPORT_COLUMNS}
     *
     * @param row The row into which data of this term will be generated
     */
    public void toExcel(Row row) {
        Objects.requireNonNull(row);
        row.createCell(0).setCellValue(getUri().toString());
        row.createCell(1).setCellValue(getLabel());
        if (definition != null) {
            row.createCell(2).setCellValue(definition);
        }
        if (comment != null) {
            row.createCell(3).setCellValue(comment);
        }
        if (types != null) {
            row.createCell(4).setCellValue(String.join(";", types));
        }
        if (sources != null) {
            row.createCell(5).setCellValue(String.join(";", sources));
        }
        if (subTerms != null) {
            row.createCell(6)
               .setCellValue(String.join(";", subTerms.stream().map(URI::toString).collect(Collectors.toSet())));
        }
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
        return Objects.equals(getUri(), term.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    @Override
    public String toString() {
        return "Term{" +
                getLabel() +
                " <" + getUri() + '>' +
                ", types=" + types +
                '}';
    }
}
