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
package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.Properties;
import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.dto.TermInfo;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.changetracking.Audited;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.apache.poi.ss.usermodel.Row;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Audited
@OWLClass(iri = SKOS.CONCEPT)
@JsonLdAttributeOrder({"uri", "label", "description", "subTerms"})
public class Term extends Asset implements HasTypes, Serializable {

    /**
     * Names of columns used in term export.
     */
    public static final List<String> EXPORT_COLUMNS = Collections
            .unmodifiableList(Arrays.asList("IRI", "Label", "Definition", "Description", "Types", "Sources", "Parent term",
                    "SubTerms"));

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLAnnotationProperty(iri = SKOS.PREF_LABEL)
    private String label;

    @OWLAnnotationProperty(iri = SKOS.SCOPE_NOTE)
    private String description;

    @OWLAnnotationProperty(iri = SKOS.DEFINITION)
    private String definition;

    @OWLAnnotationProperty(iri = DC.Terms.SOURCE, simpleLiteral = true)
    private Set<String> sources;

    @OWLObjectProperty(iri = SKOS.BROADER, fetch = FetchType.EAGER)
    private Set<Term> parentTerms;

    @Transient  // Not used by JOPA
    @OWLObjectProperty(iri = SKOS.NARROWER) // But map the property for JSON-LD serialization
    private Set<TermInfo> subTerms;

    @OWLObjectProperty(iri = Vocabulary.s_p_je_pojmem_ze_slovniku)
    private URI vocabulary;

    @Properties(fetchType = FetchType.EAGER)
    private Map<String, Set<String>> properties;

    @Types
    private Set<String> types;

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Set<TermInfo> getSubTerms() {
        return subTerms;
    }

    public void setSubTerms(Set<TermInfo> subTerms) {
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
        sb.append(',').append(CsvUtils.sanitizeString(description));
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
            sb.append(
                    exportCollection(subTerms.stream().map(ti -> ti.getUri().toString()).collect(Collectors.toSet())));
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
        if (description != null) {
            row.createCell(3).setCellValue(description);
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
               .setCellValue(String.join(";",
                       subTerms.stream().map(ti -> ti.getUri().toString()).collect(Collectors.toSet())));
        }
    }

    /**
     * Checks whether this term has a parent term in the same vocabulary.
     *
     * @return Whether this term has parent in its vocabulary. Returns {@code false} also if this term has no parent
     * term at all
     */
    public boolean hasParentInSameVocabulary() {
        return parentTerms != null && parentTerms.stream().anyMatch(p -> p.getVocabulary().equals(vocabulary));
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
