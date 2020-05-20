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
package cz.cvut.kbss.termit.persistence;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.descriptors.ObjectPropertyCollectionDescriptor;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

/**
 * Provides descriptors for working with repository contexts.
 */
@Component
public class DescriptorFactory {

    private final PersistenceUtils persistenceUtils;

    @Autowired
    public DescriptorFactory(PersistenceUtils persistenceUtils) {
        this.persistenceUtils = persistenceUtils;
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
    public Descriptor vocabularyDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        final EntityDescriptor descriptor = assetDescriptor(vocabulary.getUri());
        descriptor.addAttributeDescriptor(Vocabulary.getGlossaryField(), glossaryDescriptor(vocabulary));
        descriptor.addAttributeDescriptor(DocumentVocabulary.getDocumentField(), documentDescriptor(vocabulary));
        return descriptor;
    }

    private EntityDescriptor assetDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        return new EntityDescriptor(persistenceUtils.resolveVocabularyContext(vocabularyUri));
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
    public Descriptor vocabularyDescriptor(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        final EntityDescriptor descriptor = assetDescriptor(vocabularyUri);
        descriptor.addAttributeDescriptor(Vocabulary.getGlossaryField(), glossaryDescriptor(vocabularyUri));
        descriptor.addAttributeDescriptor(DocumentVocabulary.getDocumentField(), documentDescriptor(vocabularyUri));
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link Document} related to the specified vocabulary (presumably a {@link
     * DocumentVocabulary}).
     * <p>
     * This means that the context of the Document (and all its relevant attributes) is given by the specified
     * vocabulary's IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Document descriptor
     */
    public Descriptor documentDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return documentDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link Document} related to a vocabulary with the specified identifier
     * (presumably of a {@link DocumentVocabulary}).
     * <p>
     * This means that the context of the Document (and all its relevant attributes) is given by the specified IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Document descriptor
     */
    public Descriptor documentDescriptor(URI vocabularyUri) {
        final EntityDescriptor descriptor = assetDescriptor(vocabularyUri);
        final Descriptor fileDescriptor = fileDescriptor(vocabularyUri);
        descriptor.addAttributeDescriptor(Document.getFilesField(), fileDescriptor);
        // Vocabulary field is inferred, so it cannot be in any specific context
        descriptor.addAttributeContext(Document.getVocabularyField(), null);
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.resource.File} related to the specified
     * vocabulary.
     * <p>
     * This means that the context of the File (and all its relevant attributes) is given by the specified vocabulary's
     * IRI.
     * <p>
     * Note that default context is used for asset author and last editor.
     *
     * @param vocabulary Vocabulary identifier on which the descriptor should be based
     * @return File descriptor
     */
    public Descriptor fileDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return fileDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.resource.File} related to a vocabulary with the
     * specified identifier.
     * <p>
     * This means that the context of the File (and all its relevant attributes) is given by the specified IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return File descriptor
     */
    public Descriptor fileDescriptor(URI vocabularyUri) {
        final Descriptor descriptor = assetDescriptor(vocabularyUri);
        final Descriptor docDescriptor = assetDescriptor(vocabularyUri);
        docDescriptor.addAttributeDescriptor(Document.getFilesField(), assetDescriptor(vocabularyUri));
        descriptor.addAttributeDescriptor(File.getDocumentField(), docDescriptor);
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Glossary} related to the specified vocabulary.
     * <p>
     * This means that the context of the Glossary (and all its relevant attributes) is given by the specified
     * vocabulary's IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Glossary descriptor
     */
    public Descriptor glossaryDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return glossaryDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Glossary} related to a vocabulary with the
     * specified identifier.
     * <p>
     * This means that the context of the Glossary (and all its relevant attributes) is given by the specified IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Glossary descriptor
     */
    public Descriptor glossaryDescriptor(URI vocabularyUri) {
        final EntityDescriptor descriptor = assetDescriptor(vocabularyUri);
        descriptor.addAttributeDescriptor(Glossary.getTermsField(), termDescriptor(vocabularyUri));
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Term} contained in the specified vocabulary.
     * <p>
     * This means that the context of the Term (and all its relevant attributes) is given by the specified vocabulary's
     * IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabulary Vocabulary on which the descriptor should be based
     * @return Term descriptor
     */
    public Descriptor termDescriptor(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        return termDescriptor(vocabulary.getUri());
    }

    /**
     * Creates a JOPA descriptor for a {@link cz.cvut.kbss.termit.model.Term} contained in a vocabulary with the
     * specified identifier.
     * <p>
     * This means that the context of the Term (and all its relevant attributes) is given by the specified vocabulary
     * IRI.
     * <p>
     * Note that default context is used for asset author.
     *
     * @param vocabularyUri Vocabulary identifier on which the descriptor should be based
     * @return Term descriptor
     */
    public Descriptor termDescriptor(URI vocabularyUri) {
        final EntityDescriptor descriptor = assetDescriptor(vocabularyUri);
        descriptor.addAttributeDescriptor(Term.getParentTermsField(), assetDescriptor(vocabularyUri));
        return descriptor;
    }

    /**
     * Creates a JOPA descriptor for the specified term.
     * <p>
     * This takes the context from the term's vocabulary. Note that if parent terms are provided for the term, their
     * vocabularies are used as their contexts.
     *
     * @param term Term to create descriptor for
     * @return Term descriptor
     */
    public Descriptor termDescriptor(Term term) {
        Objects.requireNonNull(term);
        assert term.getVocabulary() != null;
        final EntityDescriptor descriptor = assetDescriptor(term.getVocabulary());
        if (term.getParentTerms() != null && !term.getParentTerms().isEmpty()) {
            // NOTE: This does not support parents being from multiple different vocabularies. Such situation is currently not supported by JOPA
            final URI parentVocabulary = persistenceUtils.resolveVocabularyContext(term.getParentTerms().iterator().next().getVocabulary());
            final ObjectPropertyCollectionDescriptor opDescriptor = new ObjectPropertyCollectionDescriptor(
                    parentVocabulary, Term.getParentTermsField());
            descriptor.addAttributeDescriptor(Term.getParentTermsField(), opDescriptor);
        } else {
            descriptor.addAttributeDescriptor(Term.getParentTermsField(), assetDescriptor(term.getVocabulary()));
        }
        return descriptor;
    }
}
