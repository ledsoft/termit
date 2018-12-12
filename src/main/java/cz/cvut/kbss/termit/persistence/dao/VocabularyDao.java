package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.util.MetamodelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class VocabularyDao extends BaseDao<Vocabulary> {

    private final MetamodelUtils metamodelUtils;

    @Autowired
    public VocabularyDao(EntityManager em, MetamodelUtils metamodelUtils) {
        super(Vocabulary.class, em);
        this.metamodelUtils = metamodelUtils;
    }

    @Override
    public List<Vocabulary> findAll() {
        final List<Vocabulary> result = super.findAll();
        result.sort(Comparator.comparing(Vocabulary::getName));
        return result;
    }

    @Override
    public void persist(Vocabulary entity) {
        Objects.requireNonNull(entity);
        try {
            em.persist(entity, descriptorFor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    private Descriptor descriptorFor(Vocabulary entity) {
        final EntityDescriptor descriptor = new EntityDescriptor(entity.getUri());
        descriptor.addAttributeDescriptor(metamodelUtils.getMappedField(Vocabulary.class, "author"),
                new EntityDescriptor(null));
        final EntityDescriptor docDescriptor = new EntityDescriptor(entity.getUri());
        docDescriptor
                .addAttributeDescriptor(metamodelUtils.getMappedField(Document.class, "author"),
                        new EntityDescriptor(null));
        final EntityDescriptor fileDescriptor = new EntityDescriptor(entity.getUri());
        fileDescriptor.addAttributeDescriptor(metamodelUtils.getMappedField(File.class, "author"),
                new EntityDescriptor(null));
        docDescriptor
                .addAttributeDescriptor(em.getMetamodel().entity(Document.class).getAttribute("files").getJavaField(),
                        fileDescriptor);
        descriptor.addAttributeDescriptor(
                em.getMetamodel().entity(DocumentVocabulary.class).getAttribute("document").getJavaField(),
                docDescriptor);
        return descriptor;
    }

    @Override
    public Vocabulary update(Vocabulary entity) {
        Objects.requireNonNull(entity);
        try {
            // Evict possibly cached instance loaded from default context
            em.getEntityManagerFactory().getCache().evict(Vocabulary.class, entity.getUri(), null);
            return em.merge(entity, descriptorFor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
