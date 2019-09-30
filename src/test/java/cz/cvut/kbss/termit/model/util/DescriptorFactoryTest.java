package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DescriptorFactoryTest {

    private Vocabulary vocabulary = Generator.generateVocabularyWithId();

    private Term term;

    private FieldSpecification parentFieldSpec;

    @BeforeEach
    void setUp() {
        this.term = Generator.generateTermWithId();
        term.setVocabulary(vocabulary.getUri());
        this.parentFieldSpec = mock(FieldSpecification.class);
        when(parentFieldSpec.getJavaField()).thenReturn(Term.getParentTermsField());
    }

    @Test
    void termDescriptorCreatesSimpleTermDescriptorWhenNoParentsAreProvided() {
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(vocabulary.getUri(), result.getAttributeContext(parentFieldSpec));
    }

    @Test
    void termDescriptorCreatesSimpleTermDescriptorWhenParentsAreInSameVocabulary() {
        final Term parent = Generator.generateTermWithId();
        parent.setVocabulary(vocabulary.getUri());
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(vocabulary.getUri(), result.getAttributeContext(parentFieldSpec));
    }

    @Test
    void termDescriptorCreatesDescriptorWithParentTermContextCorrespondingToParentTermVocabulary() {
        final Term parent = Generator.generateTermWithId();
        final URI parentVocabulary = Generator.generateUri();
        parent.setVocabulary(parentVocabulary);
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        assertEquals(vocabulary.getUri(), result.getContext());
        assertEquals(parentVocabulary, result.getAttributeDescriptor(parentFieldSpec).getContext());
    }

    @Test
    void parentTermDescriptorContainedInTermDescriptorHasCorrectAuthorAndLastEditorContext() {
        final Term parent = Generator.generateTermWithId();
        final URI parentVocabulary = Generator.generateUri();
        parent.setVocabulary(parentVocabulary);
        term.addParentTerm(parent);
        final Descriptor result = DescriptorFactory.termDescriptor(term);
        final Descriptor parentDescriptor = result.getAttributeDescriptor(parentFieldSpec);
        assertEquals(parentVocabulary, parentDescriptor.getContext());
        final FieldSpecification authorFieldSpec = mock(FieldSpecification.class);
        when(authorFieldSpec.getJavaField()).thenReturn(HasProvenanceData.getAuthorField());
        assertNull(parentDescriptor.getAttributeDescriptor(authorFieldSpec).getContext());
        final FieldSpecification lastEditorFieldSpec = mock(FieldSpecification.class);
        when(lastEditorFieldSpec.getJavaField()).thenReturn(HasProvenanceData.getLastEditorField());
        assertNull(parentDescriptor.getAttributeDescriptor(lastEditorFieldSpec).getContext());
    }
}