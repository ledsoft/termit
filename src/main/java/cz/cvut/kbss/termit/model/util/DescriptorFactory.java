package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;

import java.net.URI;
import java.util.Objects;

/**
 * Provides descriptors for working with repository contexts.
 */
public class DescriptorFactory {

    private DescriptorFactory() {
        throw new AssertionError();
    }

    /**
     * Creates a JOPA descriptor for the specified vocabulary.
     * <p>
     * The descriptor specifies that the instance context will correspond to the {@code vocabulary}'s IRI. It also
     * initializes other required attribute descriptors.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary for which the descriptor should be created
     * @return Vocabulary descriptor
     */
    public static Descriptor vocabularyDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final EntityDescriptor descriptor = new EntityDescriptor(vocabulary.getUri());
        addAuthorAndEditorDescriptors(descriptor);
        descriptor.addAttributeDescriptor(Vocabulary.getGlossaryField(), glossaryDescriptor(vocabulary));
        descriptor.addAttributeDescriptor(DocumentVocabulary.getDocumentField(), documentDescriptor(vocabulary));
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a vocabulary with the specified identifier.
     * <p>
     * The descriptor specifies that the instance context will correspond to the given IRI. It also initializes other
     * required attribute descriptors.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier for which the descriptor should be created
     * @return Vocabulary descriptor
     */
    public static Descriptor vocabularyDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final EntityDescriptor descriptor = new EntityDescriptor(vocabularyUri);
        addAuthorAndEditorDescriptors(descriptor);
        descriptor.addAttributeDescriptor(Vocabulary.getGlossaryField(), glossaryDescriptor(vocabularyUri));
        descriptor.addAttributeDescriptor(DocumentVocabulary.getDocumentField(), documentDescriptor(vocabularyUri));
        return descriptor;
    }

    private static void addAuthorAndEditorDescriptors(EntityDescriptor targetDescriptor) {
        targetDescriptor.addAttributeDescriptor(HasProvenanceData.getAuthorField(), new EntityDescriptor(null));
        targetDescriptor.addAttributeDescriptor(HasProvenanceData.getLastEditorField(), new EntityDescriptor(null));
    }

    /**
     * Creates a JOPA descriptor for a {@link Document} related to the specified vocabulary (presumably a {@link
     * DocumentVocabulary}).
     * <p>
     * This means that the context of the document (and all its relevant attributes) is given by the specified
     * vocabulary's IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Document descriptor
     */
    public static Descriptor documentDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return documentDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link Document} related to a vocabulary with the specified identifier
     * (presumably of a {@link DocumentVocabulary}).
     * <p>
     * This means that the context of the document (and all its relevant attributes) is given by the specified IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Document descriptor
     */
    public static Descriptor documentDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final EntityDescriptor descriptor = new EntityDescriptor(vocabularyUri);
        addAuthorAndEditorDescriptors(descriptor);
        final EntityDescriptor fileDescriptor = new EntityDescriptor(vocabularyUri);
        addAuthorAndEditorDescriptors(fileDescriptor);
        descriptor.addAttributeDescriptor(Document.getFilesField(), fileDescriptor);
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Glossary} related to the specified vocabulary.
     * <p>
     * This means that the context of the glossary (and all its relevant attributes) is given by the specified
     * vocabulary's IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Glossary descriptor
     */
    public static Descriptor glossaryDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return glossaryDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Glossary} related to a vocabulary with the
     * specified identifier.
     * <p>
     * This means that the context of the glossary (and all its relevant attributes) is given by the specified IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Glossary descriptor
     */
    public static Descriptor glossaryDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final EntityDescriptor descriptor = new EntityDescriptor(vocabularyUri);
        addAuthorAndEditorDescriptors(descriptor);
        descriptor.addAttributeDescriptor(Glossary.getTermsField(), termDescriptor(vocabularyUri));
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Term} contained in the specified vocabulary.
     * <p>
     * This means that the context of the term (and all its relevant attributes) is given by the specified vocabulary's
     * IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Term descriptor
     */
    public static Descriptor termDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return termDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Term} contained in a vocabulary with the
     * specified identifier.
     * <p>
     * This means that the context of the term (and all its relevant attributes) is given by the specified vocabulary
     * IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Term descriptor
     */
    public static Descriptor termDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final EntityDescriptor descriptor = new EntityDescriptor(vocabularyUri);
        addAuthorAndEditorDescriptors(descriptor);
        return descriptor;
    }
}
