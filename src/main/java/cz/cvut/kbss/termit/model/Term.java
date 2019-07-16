package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.Properties;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.RDFS;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.service.provenance.ProvenanceManager;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.poi.ss.usermodel.Row;

import java.io.Serializable;
import java.lang.reflect.Field;
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
            .unmodifiableList(Arrays.asList("IRI", "Label", "Definition", "Comment", "Types", "Sources", "Parent term",
                    "SubTerms"));

    @OWLAnnotationProperty(iri = RDFS.COMMENT)
    private String comment;

    @OWLAnnotationProperty(iri = SKOS.DEFINITION)
    private String definition;

    @OWLDataProperty(iri = DC.Elements.SOURCE)
    private Set<String> sources;

    @OWLObjectProperty(iri = SKOS.BROADER, fetch = FetchType.EAGER)
    private Set<Term> parentTerms;

    @Inferred
    @OWLObjectProperty(iri = SKOS.NARROWER)
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

    public Set<Term> getParentTerms() {
        return parentTerms;
    }

    public void setParentTerms(Set<Term> parentTerms) {
        this.parentTerms = parentTerms;
    }

    public void addParentTerm(Term term) {
        if (parentTerms == null) {
            this.parentTerms = new HashSet<>();
        }
        parentTerms.add(term);
    }

    public Set<URI> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<URI> subTerms) {
        this.subTerms = subTerms;
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
        sb.append(',').append(CsvUtils.sanitizeString(definition));
        sb.append(',').append(CsvUtils.sanitizeString(comment));
        sb.append(',');
        if (types != null && !types.isEmpty()) {
            sb.append(exportCollection(types));
        }
        sb.append(',');
        if (sources != null && !sources.isEmpty()) {
            sb.append(exportCollection(sources));
        }
        sb.append(',');
        if (parentTerms != null && !parentTerms.isEmpty()) {
            sb.append(exportCollection(
                    parentTerms.stream().map(pt -> pt.getUri().toString()).collect(Collectors.toSet())));
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
        if (parentTerms != null) {
            row.createCell(6)
               .setCellValue(String.join(";",
                       parentTerms.stream().map(pt -> pt.getUri().toString()).collect(Collectors.toSet())));
        }
        if (subTerms != null) {
            row.createCell(7)
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

    public static Field getParentTermsField() {
        try {
            return Term.class.getDeclaredField("parentTerms");
        } catch (NoSuchFieldException e) {
            throw new TermItException("Fatal error! Unable to retrieve \"parentTerms\" field.", e);
        }
    }
}
